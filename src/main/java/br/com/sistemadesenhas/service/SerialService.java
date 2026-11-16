package br.com.sistemadesenhas.service;

import br.com.sistemadesenhas.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * Gera o serial de cada empresa, LIGADO AO CNPJ.
 * <p>
 * Como funciona: serial = HMAC-SHA256(segredo, "cnpj:versao"), pegando os 16 primeiros caracteres
 * e formatando como XXXX-XXXX-XXXX-XXXX. Só quem conhece o segredo (o servidor) consegue gerar um serial
 * válido, e o mesmo CNPJ com a mesma versão sempre gera o mesmo serial.
 * <p>
 * A "versao" permite ao GM gerar um serial novo para o mesmo CNPJ (ex.: se o antigo vazou).
 */
@Service
@RequiredArgsConstructor
public class SerialService {

    private final AppProperties app;

    /**
     * @param cnpj   CNPJ só com números
     * @param versao versão do serial (começa em 1)
     * @return serial no formato XXXX-XXXX-XXXX-XXXX
     */
    public String gerar(String cnpj, int versao) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(app.serial().segredo().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] resultado = hmac.doFinal((cnpj + ":" + versao).getBytes(StandardCharsets.UTF_8));
            String hex = HexFormat.of().withUpperCase().formatHex(resultado).substring(0, 16);
            return hex.substring(0, 4) + "-" + hex.substring(4, 8) + "-" + hex.substring(8, 12) + "-" + hex.substring(12, 16);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível gerar o serial", e);
        }
    }
}
