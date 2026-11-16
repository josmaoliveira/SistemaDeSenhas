package br.com.sistemadesenhas.dto;

import java.math.BigDecimal;

/** Mensalidade padrão do sistema, para o painel do GM. */
public record MensalidadeResponse(BigDecimal valorMensalPadrao) {
}
