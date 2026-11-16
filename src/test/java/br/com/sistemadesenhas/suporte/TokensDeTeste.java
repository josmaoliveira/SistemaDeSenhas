package br.com.sistemadesenhas.suporte;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Nos testes não dá para fazer login de verdade no Firebase. Então geramos tokens IGUAIS aos do Firebase
 * (mesmos campos), assinados com uma chave criada aqui, e trocamos o validador de tokens por um que
 * confia nessa chave. O resto do sistema (conversor de perfil, regras de acesso) roda de verdade.
 */
@TestConfiguration
public class TokensDeTeste {

    private static final KeyPair CHAVES = gerarChaves();

    @Bean
    @Primary
    public JwtDecoder jwtDecoderDeTeste() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) CHAVES.getPublic()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("https://securetoken.google.com/projeto-teste"));
        return decoder;
    }

    /** Token de quem entrou com e-mail e senha. */
    public static String email(String uid, String email, boolean verificado) {
        return gerar(uid, Map.of("email", email, "email_verified", verificado), "password");
    }

    /** Token de quem entrou com o Google (e-mail sempre verificado). */
    public static String google(String uid, String email) {
        return gerar(uid, Map.of("email", email, "email_verified", true, "name", "Pessoa Google"), "google.com");
    }

    /** Token de quem entrou por SMS. */
    public static String sms(String uid, String telefone) {
        return gerar(uid, Map.of("phone_number", telefone), "phone");
    }

    private static String gerar(String uid, Map<String, Object> extras, String provedor) {
        try {
            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                    .subject(uid)
                    .issuer("https://securetoken.google.com/projeto-teste")
                    .audience("projeto-teste")
                    .issueTime(new Date())
                    .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                    .claim("firebase", new HashMap<>(Map.of("sign_in_provider", provedor)));
            extras.forEach(claims::claim);
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims.build());
            jwt.sign(new RSASSASigner(CHAVES.getPrivate()));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private static KeyPair gerarChaves() {
        try {
            KeyPairGenerator gerador = KeyPairGenerator.getInstance("RSA");
            gerador.initialize(2048);
            return gerador.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
