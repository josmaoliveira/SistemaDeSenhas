package br.com.sistemadesenhas.security;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Transforma o token de login (JWT) em uma {@link Identidade}.
 * <p>
 * Cada serviço de login coloca as informações no token de um jeito diferente. Esta interface
 * isola isso: hoje existe só a implementação do Firebase ({@link LeitorDeIdentidadeFirebase}).
 * Para trocar de serviço (Auth0, Keycloak, Cognito...), cria-se outra implementação e
 * nenhum service precisa mudar. Veja docs/FIREBASE.md, seção "Trocar o serviço de login".
 */
public interface LeitorDeIdentidade {

    Identidade ler(Jwt token);
}
