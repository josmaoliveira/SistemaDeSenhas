package br.com.sistemadesenhas.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Tudo que o back-end precisa saber do Firebase para validar o login.
 * <p>
 * Trocar o serviço de login = trocar este decodificador (veja docs/FIREBASE.md).
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    /** Endereço onde o Google publica as chaves públicas que assinam os tokens do Firebase. */
    private static final String CHAVES_FIREBASE =
            "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

    /**
     * Decodificador que valida os tokens do Firebase:
     * assinatura do Google, validade (exp), emissor ("iss") e destinatário ("aud") = nosso projeto.
     */
    @Bean
    public JwtDecoder jwtDecoder(FirebaseProperties firebase) {
        if (!firebase.configurado()) {
            log.warn("Firebase NÃO configurado (firebase.project-id / firebase.api-key). O login não vai funcionar. "
                    + "Veja application-local.properties.exemplo");
        }
        String projeto = firebase.projectId() == null ? "" : firebase.projectId();

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(CHAVES_FIREBASE).build();
        OAuth2TokenValidator<Jwt> emissor = JwtValidators.createDefaultWithIssuer("https://securetoken.google.com/" + projeto);
        OAuth2TokenValidator<Jwt> destinatario = token -> token.getAudience().contains(projeto)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Token de outro projeto", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(emissor, destinatario));
        return decoder;
    }
}
