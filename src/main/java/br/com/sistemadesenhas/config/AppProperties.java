package br.com.sistemadesenhas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Valores "app.*" do application.properties (regras do sistema).
 *
 * @param gmEmails e-mails, separados por vírgula, que viram GM no primeiro login
 * @param licenca  regras da licença
 * @param serial   configuração do serial
 */
@ConfigurationProperties("app")
public record AppProperties(String gmEmails, Licenca licenca, Serial serial) {

    /** @param diasGratis dias de uso grátis para empresas novas */
    public record Licenca(int diasGratis) {
    }

    /** @param segredo texto secreto que entra no cálculo do serial */
    public record Serial(String segredo) {
    }

    /** @return e-mails de GM em minúsculas, sem espaços */
    public Set<String> listaEmailsGm() {
        if (gmEmails == null || gmEmails.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(gmEmails.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }
}
