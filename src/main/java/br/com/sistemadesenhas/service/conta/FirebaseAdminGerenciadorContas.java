package br.com.sistemadesenhas.service.conta;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import br.com.sistemadesenhas.exception.ErroNegocio;
import org.springframework.http.HttpStatus;

/**
 * Estratégia "completa": usa o Firebase Admin SDK, autenticado com a conta de serviço.
 * O envio de e-mail de redefinição é repassado para a estratégia REST (o Admin SDK só gera o link,
 * não envia o e-mail).
 */
public class FirebaseAdminGerenciadorContas implements GerenciadorContas {

    private final FirebaseAuth auth;
    private final GerenciadorContas envioDeEmail;

    public FirebaseAdminGerenciadorContas(FirebaseApp app, GerenciadorContas envioDeEmail) {
        this.auth = FirebaseAuth.getInstance(app);
        this.envioDeEmail = envioDeEmail;
    }

    @Override
    public String nome() {
        return "Firebase Admin (completo)";
    }

    @Override
    public boolean suportaAlteracaoDireta() {
        return true;
    }

    @Override
    public void enviarEmailRedefinicaoSenha(String email) {
        envioDeEmail.enviarEmailRedefinicaoSenha(email);
    }

    @Override
    public void definirSenha(String uid, String novaSenha) {
        executar(() -> auth.updateUser(new UserRecord.UpdateRequest(uid).setPassword(novaSenha)));
    }

    @Override
    public void definirAtiva(String uid, boolean ativa) {
        executar(() -> auth.updateUser(new UserRecord.UpdateRequest(uid).setDisabled(!ativa)));
    }

    @Override
    public void excluirConta(String uid) {
        executar(() -> {
            auth.deleteUser(uid);
            return null;
        });
    }

    /** Executa a chamada ao Firebase e transforma o erro dele em uma mensagem amigável. */
    private void executar(ChamadaFirebase chamada) {
        try {
            chamada.executar();
        } catch (FirebaseAuthException e) {
            throw new ErroNegocio(HttpStatus.BAD_GATEWAY, "FIREBASE_ERRO", "Firebase: " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface ChamadaFirebase {
        Object executar() throws FirebaseAuthException;
    }
}
