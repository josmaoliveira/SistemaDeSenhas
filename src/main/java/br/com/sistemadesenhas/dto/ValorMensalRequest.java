package br.com.sistemadesenhas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

/**
 * Valor da mensalidade. Para uma empresa, vazio (null) = voltar a pagar o valor padrão.
 */
public record ValorMensalRequest(
        @DecimalMin(value = "0.01", message = "A mensalidade deve ser maior que zero.")
        @Digits(integer = 8, fraction = 2, message = "Valor inválido.") BigDecimal valorMensal) {
}
