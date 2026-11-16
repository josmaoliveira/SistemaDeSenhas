package br.com.sistemadesenhas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Valores "firebase.*" do application.properties.
 *
 * @param projectId     id do projeto no Firebase (ex.: sistema-senhas-123)
 * @param apiKey        chave da API Web (é pública: aparece no navegador, não é segredo)
 * @param authDomain    domínio de login (normalmente projectId.firebaseapp.com)
 * @param appId         id do app Web
 * @param credenciais   caminho do JSON da conta de serviço (opcional, ESTE sim é segredo)
 * @param smsHabilitado mostra as opções de SMS nas telas
 */
@ConfigurationProperties("firebase")
public record FirebaseProperties(String projectId, String apiKey, String authDomain, String appId,
                                 String credenciais, boolean smsHabilitado) {

    /** @return true se o mínimo para o login funcionar foi preenchido */
    public boolean configurado() {
        return temTexto(projectId) && temTexto(apiKey);
    }

    /** Se o authDomain não foi informado, usa o padrão do Firebase. */
    public String authDomainEfetivo() {
        return temTexto(authDomain) ? authDomain : projectId + ".firebaseapp.com";
    }

    public boolean temCredenciais() {
        return temTexto(credenciais);
    }

    private static boolean temTexto(String s) {
        return s != null && !s.isBlank();
    }
}
