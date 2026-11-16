// =====================================================================
// app.js - funções usadas por TODAS as telas
//
// - api(): chama o nosso servidor já mandando o token do usuário logado
// - exigirLogin(): protege as telas restritas e diz quem está logado
// - Pequenos utilitários (avisos, formatação, proteção contra HTML malicioso)
//
// Como usar numa página:
//   <script type="module">
//     import { exigirLogin, api } from '/js/app.js';
//     const eu = await exigirLogin(['ADMIN_EMPRESA']);
//   </script>
// =====================================================================

// O login fica todo em autenticacao.js (o único arquivo que conhece o Firebase)
import { iniciarAutenticacao, aguardarUsuario, tokenDeAcesso, sairDaConta } from '/js/autenticacao.js';

/** Para onde cada perfil vai depois do login. */
export const PAGINA_INICIAL = {
    GM: '/gm.html',
    ADMIN_EMPRESA: '/admin.html',
    ATENDENTE: '/selecionar-guiche.html'
};

/** Erro devolvido pela nossa API: tem status HTTP, código e mensagem. */
export class ErroApi extends Error {
    constructor(status, codigo, mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }
}

/**
 * Chama o nosso servidor.
 * - Se houver alguém logado, manda o cabeçalho "Authorization: Bearer <token do login>".
 * - Converte o corpo para JSON e a resposta de volta.
 * - Em caso de erro, lança ErroApi com a mensagem que o servidor mandou.
 *
 * @param url     ex.: '/api/empresa/servicos'
 * @param opcoes  { metodo: 'POST', corpo: {...} }
 */
export async function api(url, { metodo = 'GET', corpo } = {}) {
    const cabecalhos = {};
    if (corpo !== undefined) cabecalhos['Content-Type'] = 'application/json';
    const token = await tokenDeAcesso();
    if (token) cabecalhos['Authorization'] = 'Bearer ' + token;

    const resposta = await fetch(url, {
        method: metodo,
        headers: cabecalhos,
        body: corpo !== undefined ? JSON.stringify(corpo) : undefined
    });

    if (resposta.status === 204) return null;

    const texto = await resposta.text();
    let dados = null;
    try { dados = texto ? JSON.parse(texto) : null; } catch { dados = null; }

    if (!resposta.ok) {
        if (resposta.status === 401) {
            throw new ErroApi(401, 'NAO_AUTENTICADO', 'Sua sessão expirou. Faça login novamente.');
        }
        if (resposta.status === 403 && !dados) {
            throw new ErroApi(403, 'ACESSO_NEGADO', 'Você não tem permissão para isso.');
        }
        throw new ErroApi(resposta.status, dados?.codigo, dados?.mensagem || `Erro ${resposta.status}`);
    }
    return dados;
}

/**
 * Protege uma tela: só deixa ficar quem está logado E tem um dos perfis informados.
 * Quem não está logado vai para o login; quem tem outro perfil vai para a sua tela inicial.
 *
 * @param perfisPermitidos ex.: ['ADMIN_EMPRESA']
 * @returns os dados do usuário (nome, perfil, empresa) vindos de /api/auth/me
 */
export async function exigirLogin(perfisPermitidos) {
    await iniciarAutenticacao();
    if (!await aguardarUsuario()) {
        await irPara('/login.html');
    }
    let eu;
    try {
        eu = await api('/api/auth/me');
    } catch (erro) {
        await irPara('/login.html?erro=' + encodeURIComponent(erro.message));
    }
    if (!perfisPermitidos.includes(eu.perfil)) {
        await irPara(PAGINA_INICIAL[eu.perfil] || '/login.html');
    }
    // Licença vencida/bloqueada: só o administrador entra (para ver a licença na gestão)
    if (eu.licenca && !eu.licenca.liberada && eu.perfil !== 'ADMIN_EMPRESA') {
        await sairDaConta();
        await irPara('/login.html?erro=' + encodeURIComponent(
            'O acesso da empresa está suspenso (licença ' + NOME_SITUACAO[eu.licenca.situacao].toLowerCase() +
            '). Fale com o administrador da sua empresa.'));
    }
    return eu;
}

/** Sai do sistema e volta para o login. */
export async function sair() {
    await iniciarAutenticacao();
    await sairDaConta();
    localStorage.removeItem('guicheId');
    window.location.href = '/login.html';
}

/**
 * Muda de página. Use com "await": a promessa nunca termina, então o código
 * que vem depois não roda enquanto o navegador troca de página.
 */
export function irPara(url) {
    window.location.href = url;
    return new Promise(() => {});
}

/**
 * Protege contra HTML malicioso: troca < > & " ' por códigos seguros.
 * SEMPRE use ao colocar texto digitado por alguém dentro de innerHTML.
 */
export function esc(valor) {
    return String(valor ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}

/** Mostra um aviso no canto da tela por alguns segundos. tipo: 'ok' ou 'erro' */
export function avisar(mensagem, tipo = 'ok') {
    let caixa = document.getElementById('aviso-flutuante');
    if (!caixa) {
        caixa = document.createElement('div');
        caixa.id = 'aviso-flutuante';
        document.body.appendChild(caixa);
    }
    caixa.textContent = mensagem;
    caixa.className = tipo;
    caixa.classList.remove('oculto');
    clearTimeout(caixa._timer);
    caixa._timer = setTimeout(() => caixa.classList.add('oculto'), 4000);
}

/** "2026-10-25T14:30:00" -> "25/10/2026 14:30" */
export function dataHora(iso) {
    if (!iso) return '-';
    return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

/** Nome amigável da situação da licença. */
export const NOME_SITUACAO = {
    EM_TESTE: 'Período grátis',
    ATIVA: 'Ativa',
    VENCIDA: 'Vencida',
    BLOQUEADA: 'Bloqueada'
};

/** "2026-10-25" -> "25/10/2026" (sem mudar o dia por causa do fuso horário) */
export function data(iso) {
    if (!iso) return '-';
    const [ano, mes, dia] = iso.split('-');
    return `${dia}/${mes}/${ano}`;
}

/** Nome amigável dos perfis. */
export const NOME_PERFIL = {
    GM: 'GM (administrador geral)',
    ADMIN_EMPRESA: 'Administrador',
    ATENDENTE: 'Atendente'
};

/**
 * Coloca um botão "Copiar" ao lado de cada elemento com o atributo data-copiar.
 * O botão copia o texto que o elemento tiver NA HORA do clique (pode ser preenchido depois).
 * Pode ser chamada de novo depois de redesenhar uma tabela: elementos que já têm botão são pulados.
 *
 * @param raiz onde procurar (padrão: a página inteira)
 */
export function ativarBotoesCopiar(raiz = document) {
    raiz.querySelectorAll('[data-copiar]:not([data-copiar-ativo])').forEach(elemento => {
        elemento.dataset.copiarAtivo = '1';
        const botao = document.createElement('button');
        botao.type = 'button';
        botao.className = 'pequeno secundario botao-copiar';
        botao.textContent = 'Copiar';
        botao.addEventListener('click', async () => {
            const copiado = await copiarTexto(elemento.textContent.trim());
            avisar(copiado ? 'Copiado!' : 'Não foi possível copiar. Selecione o texto e use Ctrl+C.', copiado ? 'ok' : 'erro');
        });
        elemento.after(botao);
    });
}

/**
 * Copia um texto para a área de transferência.
 * O jeito moderno (navigator.clipboard) só funciona em https ou localhost; fora disso usa o jeito antigo.
 * @returns true se conseguiu copiar
 */
export async function copiarTexto(texto) {
    try {
        if (navigator.clipboard && window.isSecureContext) {
            await navigator.clipboard.writeText(texto);
            return true;
        }
    } catch { /* tenta o jeito antigo abaixo */ }
    const campo = document.createElement('textarea');
    campo.value = texto;
    campo.style.position = 'fixed';
    campo.style.opacity = '0';
    document.body.appendChild(campo);
    campo.select();
    const ok = document.execCommand('copy');
    campo.remove();
    return ok;
}

/** 95 -> "1 min 35 s"; null -> "-" */
export function duracao(segundos) {
    if (segundos === null || segundos === undefined) return '-';
    const min = Math.floor(segundos / 60);
    const seg = segundos % 60;
    return min > 0 ? `${min} min ${seg} s` : `${seg} s`;
}
