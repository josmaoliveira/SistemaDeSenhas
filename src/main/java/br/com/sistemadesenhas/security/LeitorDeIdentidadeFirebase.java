package br.com.sistemadesenhas.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Lê as informações que o Firebase coloca dentro do token (os "claims").
 * <p>
 * Exemplo de claims: sub (id do usuário no Firebase), email, email_verified, phone_number, name,
 * firebase.sign_in_provider (password, google.com, phone).
 * <p>
 * É a ÚNICA classe do back-end que conhece o formato do token do Firebase.
 */
@Component
public class LeitorDeIdentidadeFirebase implements LeitorDeIdentidade {

    /** Login por e-mail e senha: é o único que exige o e-mail confirmado (Google já vem verificado e SMS não usa e-mail). */
    private static final String PROVEDOR_SENHA = "password";

    @Override
    public Identidade ler(Jwt token) {
        String email = token.getClaimAsString("email");
        boolean emailVerificado = Boolean.TRUE.equals(token.getClaimAsBoolean("email_verified"));
        String provedor = provedor(token);
        return new Identidade(
                token.getSubject(),
                email == null ? null : email.trim().toLowerCase(),
                emailVerificado,
                token.getClaimAsString("phone_number"),
                token.getClaimAsString("name"),
                provedor,
                PROVEDOR_SENHA.equals(provedor) && !emailVerificado);
    }

    /** @return como a pessoa entrou: password, google.com ou phone */
    private String provedor(Jwt token) {
        Object firebase = token.getClaims().get("firebase");
        if (firebase instanceof Map<?, ?> mapa && mapa.get("sign_in_provider") != null) {
            return mapa.get("sign_in_provider").toString();
        }
        return "desconhecido";
    }
}
