package br.com.sistemadesenhas.service.conta;

import br.com.sistemadesenhas.config.FirebaseProperties;
import br.com.sistemadesenhas.exception.ErroNegocio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Estratégia "básica": fala com a API REST do Firebase usando apenas a chave pública (firebase.api-key).
 * Só consegue enviar o e-mail de redefinição de senha.
 */
@Slf4j
public class FirebaseRestGerenciadorContas implements GerenciadorContas {

    private static final String URL_ENVIAR_EMAIL = "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=";

    private final FirebaseProperties firebase;
    private final RestClient http = RestClient.create();

    public FirebaseRestGerenciadorContas(FirebaseProperties firebase) {
        this.firebase = firebase;
    }

    @Override
    public String nome() {
        return "Firebase (somente e-mail de redefinição)";
    }

    @Override
    public boolean suportaAlteracaoDireta() {
        return false;
    }

    /** Chama a API "sendOobCode" com requestType PASSWORD_RESET: o Firebase envia o e-mail. */
    @Override
    public void enviarEmailRedefinicaoSenha(String email) {
        if (!firebase.configurado()) {
            throw ErroNegocio.invalido("FIREBASE_NAO_CONFIGURADO", "Configure o Firebase no application-local.properties.");
        }
        try {
            http.post()
                    .uri(URL_ENVIAR_EMAIL + firebase.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("requestType", "PASSWORD_RESET", "email", email))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Falha ao pedir e-mail de redefinição ao Firebase: {}", e.getMessage());
            throw new ErroNegocio(HttpStatus.BAD_GATEWAY, "FIREBASE_ERRO",
                    "O Firebase recusou o envio. Confira se esse e-mail já fez o primeiro acesso.");
        }
    }

    @Override
    public void definirSenha(String uid, String novaSenha) {
        throw semCredenciais();
    }

    @Override
    public void definirAtiva(String uid, boolean ativa) {
        throw semCredenciais();
    }

    @Override
    public void excluirConta(String uid) {
        throw semCredenciais();
    }

    private ErroNegocio semCredenciais() {
        return ErroNegocio.invalido("SEM_CREDENCIAIS_FIREBASE",
                "Para isso configure firebase.credenciais (conta de serviço). Use o envio de e-mail de redefinição.");
    }
}
