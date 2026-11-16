// =====================================================================
// autenticacao.js - ÚNICO arquivo das telas que conhece o Firebase
//
// As telas (login, cadastro, app.js) só chamam as funções daqui, com nomes
// em português e sem nada do Firebase. Se um dia o serviço de login mudar
// (ou o Firebase mudar a forma de usar), só ESTE arquivo precisa ser reescrito.
//
// Os erros também saem daqui já "traduzidos": ErroAutenticacao com um código
// nosso (ex.: CREDENCIAL_INVALIDA) e a mensagem em português.
// =====================================================================

// SDK oficial do Firebase, carregado direto do Google (não precisa instalar nada)
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import {
    getAuth, onAuthStateChanged, signOut, signInWithEmailAndPassword, createUserWithEmailAndPassword,
    sendEmailVerification, sendPasswordResetEmail, GoogleAuthProvider, signInWithPopup,
    RecaptchaVerifier, signInWithPhoneNumber, linkWithPhoneNumber
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";

let auth = null;
let configuracao = null;

/** Erro de login já traduzido. codigo: CREDENCIAL_INVALIDA, EMAIL_EM_USO ou OUTRO. */
export class ErroAutenticacao extends Error {
    constructor(codigo, mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }
}

/** Códigos do Firebase que as telas tratam de um jeito especial. */
const CODIGOS = {
    'auth/invalid-credential': 'CREDENCIAL_INVALIDA',
    'auth/wrong-password': 'CREDENCIAL_INVALIDA',
    'auth/user-not-found': 'CREDENCIAL_INVALIDA',
    'auth/email-already-in-use': 'EMAIL_EM_USO'
};

/** Mensagens do Firebase (em inglês, com códigos) traduzidas para português. */
const MENSAGENS = {
    'auth/invalid-credential': 'E-mail ou senha incorretos.',
    'auth/wrong-password': 'E-mail ou senha incorretos.',
    'auth/user-not-found': 'E-mail ou senha incorretos.',
    'auth/invalid-email': 'E-mail inválido.',
    'auth/email-already-in-use': 'Já existe uma conta com este e-mail. Use "Entrar" ou "Esqueci minha senha".',
    'auth/weak-password': 'A senha precisa ter pelo menos 6 caracteres.',
    'auth/too-many-requests': 'Muitas tentativas. Aguarde alguns minutos e tente de novo.',
    'auth/popup-closed-by-user': 'A janela do Google foi fechada antes de terminar.',
    'auth/popup-blocked': 'O navegador bloqueou a janela do Google. Libere pop-ups para este site.',
    'auth/invalid-phone-number': 'Telefone inválido. Use DDD + número, ex.: 83999990000.',
    'auth/invalid-verification-code': 'Código SMS incorreto.',
    'auth/code-expired': 'O código SMS expirou. Peça outro.',
    'auth/operation-not-allowed': 'Este tipo de login não foi ativado no Firebase (Console > Authentication > Método de login).',
    'auth/unauthorized-domain': 'Este endereço não está autorizado no Firebase (Authentication > Configurações > Domínios autorizados).',
    'auth/network-request-failed': 'Sem conexão com a internet.',
    'auth/requires-recent-login': 'Por segurança, saia e entre de novo antes de fazer isso.',
    'auth/provider-already-linked': 'Este telefone já está ligado à sua conta.',
    'auth/credential-already-in-use': 'Este telefone já está ligado a outra conta.'
};

/** Converte qualquer erro do Firebase em ErroAutenticacao. */
function traduzir(erro) {
    if (erro instanceof ErroAutenticacao) return erro;
    return new ErroAutenticacao(CODIGOS[erro?.code] || 'OUTRO', MENSAGENS[erro?.code] || erro?.message || 'Erro inesperado.');
}

/** Executa uma chamada ao Firebase trocando o erro dele pelo nosso. */
async function executar(acao) {
    try {
        return await acao();
    } catch (erro) {
        throw traduzir(erro);
    }
}

/** Converte "83999990000" ou "(83) 99999-0000" para o formato internacional "+5583999990000". */
function telefoneInternacional(texto) {
    const n = String(texto || '').replace(/\D/g, '');
    if (n.startsWith('55') && (n.length === 12 || n.length === 13)) return '+' + n;
    return '+55' + n;
}

/**
 * Liga o serviço de login uma única vez.
 * A configuração (apiKey, projectId...) vem de /api/publico/config, que lê o application.properties.
 */
export async function iniciarAutenticacao() {
    if (auth) return;
    const resposta = await fetch('/api/publico/config');
    configuracao = await resposta.json();
    if (!configuracao.configurado) {
        throw new Error('O Firebase ainda não foi configurado no servidor (application-local.properties).');
    }
    const app = initializeApp({
        apiKey: configuracao.apiKey,
        authDomain: configuracao.authDomain,
        projectId: configuracao.projectId,
        appId: configuracao.appId
    });
    auth = getAuth(app);
    auth.languageCode = 'pt-BR'; // e-mails e SMS em português
}

/** @returns a configuração pública (ex.: smsHabilitado) */
export function configPublica() {
    return configuracao;
}

/**
 * @returns dados simples de quem está logado agora, ou null:
 *          { email, emailConfirmado, telefone }
 */
export function usuarioAtual() {
    const u = auth?.currentUser;
    return u ? { email: u.email, emailConfirmado: u.emailVerified, telefone: u.phoneNumber } : null;
}

/**
 * Espera o serviço de login dizer se há alguém logado (ele lembra o login entre uma página e outra).
 * @returns o mesmo que usuarioAtual()
 */
export function aguardarUsuario() {
    return new Promise(resolve => {
        const parar = onAuthStateChanged(auth, () => {
            parar();
            resolve(usuarioAtual());
        });
    });
}

/**
 * @param renovar true para pedir um token novo (ex.: logo depois de confirmar o e-mail)
 * @returns o token para mandar ao nosso servidor, ou null se ninguém estiver logado
 */
export async function tokenDeAcesso(renovar = false) {
    if (!auth?.currentUser) return null;
    // Sem "renovar", o token é renovado sozinho quando está perto de vencer (dura 1 hora)
    return auth.currentUser.getIdToken(renovar);
}

export async function entrarComEmail(email, senha) {
    await executar(() => signInWithEmailAndPassword(auth, email.trim(), senha));
}

export async function entrarComGoogle() {
    await executar(() => signInWithPopup(auth, new GoogleAuthProvider()));
}

/** Cria a conta e já envia o e-mail com o link de confirmação. */
export async function criarConta(email, senha) {
    await executar(async () => {
        const credencial = await createUserWithEmailAndPassword(auth, email.trim(), senha);
        await sendEmailVerification(credencial.user);
    });
}

export async function reenviarConfirmacaoEmail() {
    await executar(() => sendEmailVerification(auth.currentUser));
}

/**
 * Confere se a pessoa já clicou no link de confirmação. Se sim, renova o token
 * (o novo já diz que o e-mail está confirmado).
 * @returns true se o e-mail já está confirmado
 */
export async function emailFoiConfirmado() {
    return executar(async () => {
        await auth.currentUser.reload();
        if (!auth.currentUser.emailVerified) return false;
        await auth.currentUser.getIdToken(true);
        return true;
    });
}

/** Envia o link para criar uma nova senha. A senha só muda quando a pessoa usa o link. */
export async function enviarRedefinicaoSenha(email) {
    await executar(() => sendPasswordResetEmail(auth, email.trim()));
}

/**
 * Envia o código por SMS.
 *
 * @param telefone       como a pessoa digitou, ex.: (83) 99999-0000
 * @param idDoElemento   id de uma <div> vazia na página (o Firebase coloca ali um reCAPTCHA invisível, contra abuso)
 * @param vincular       false = entrar com o celular; true = ligar o celular à conta de quem já está logado
 * @returns objeto com confirmar(codigo), para quando a pessoa digitar o código recebido
 */
export async function enviarCodigoSms(telefone, idDoElemento, vincular = false) {
    return executar(async () => {
        const verificador = new RecaptchaVerifier(auth, idDoElemento, { size: 'invisible' });
        const numero = telefoneInternacional(telefone);
        const confirmacao = vincular
            ? await linkWithPhoneNumber(auth.currentUser, numero, verificador)
            : await signInWithPhoneNumber(auth, numero, verificador);
        return {
            confirmar: codigo => executar(async () => {
                await confirmacao.confirm(String(codigo).trim());
                await auth.currentUser.getIdToken(true); // token novo, já com o telefone confirmado
            })
        };
    });
}

/** Sai da conta no serviço de login. */
export async function sairDaConta() {
    if (auth) await signOut(auth);
}
