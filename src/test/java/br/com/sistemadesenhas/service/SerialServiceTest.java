package br.com.sistemadesenhas.service;

import br.com.sistemadesenhas.config.AppProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Regras do serial: formato, sempre igual para o mesmo CNPJ e versão, e muda com a versão, o CNPJ ou o segredo. */
class SerialServiceTest {

    private final SerialService serial = new SerialService(
            new AppProperties(null, new AppProperties.Licenca(90), new AppProperties.Serial("segredo-de-teste")));

    @Test
    void formatoEDeterminismo() {
        String s = serial.gerar("11222333000181", 1);
        assertThat(s).matches("[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}");
        assertThat(serial.gerar("11222333000181", 1)).isEqualTo(s);       // mesmo CNPJ e versão: mesmo serial
        assertThat(serial.gerar("11222333000181", 2)).isNotEqualTo(s);    // versão nova: serial novo
        assertThat(serial.gerar("45723174000110", 1)).isNotEqualTo(s);    // outro CNPJ: outro serial
    }

    @Test
    void outroSegredoGeraOutroSerial() {
        SerialService outro = new SerialService(
                new AppProperties(null, new AppProperties.Licenca(90), new AppProperties.Serial("outro-segredo")));
        assertThat(outro.gerar("11222333000181", 1)).isNotEqualTo(serial.gerar("11222333000181", 1));
    }
}
