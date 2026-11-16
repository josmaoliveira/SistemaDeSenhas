package br.com.sistemadesenhas.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentosTest {

    @Test
    void cpfValidoEInvalido() {
        assertThat(Documentos.cpfValido("529.982.247-25")).isTrue();
        assertThat(Documentos.cpfValido("52998224725")).isTrue();
        assertThat(Documentos.cpfValido("52998224724")).isFalse();   // dígito errado
        assertThat(Documentos.cpfValido("111.111.111-11")).isFalse(); // tudo igual
        assertThat(Documentos.cpfValido("123")).isFalse();
    }

    @Test
    void cnpjValidoEInvalido() {
        assertThat(Documentos.cnpjValido("11.222.333/0001-81")).isTrue();
        assertThat(Documentos.cnpjValido("11222333000181")).isTrue();
        assertThat(Documentos.cnpjValido("11222333000182")).isFalse();
        assertThat(Documentos.cnpjValido("00000000000000")).isFalse();
    }

    @Test
    void telefoneESlug() {
        assertThat(Telefones.normalizar("(83) 99999-0000")).isEqualTo("+5583999990000");
        assertThat(Telefones.normalizar("+55 83 99999-0000")).isEqualTo("+5583999990000");
        assertThat(Telefones.normalizar("")).isNull();
        assertThat(Slugs.gerar("Padaria São João!")).isEqualTo("padaria-sao-joao");
    }
}
