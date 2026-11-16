package br.com.sistemadesenhas.dto;

import br.com.sistemadesenhas.entity.enums.FormaPagamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Pagamento registrado manualmente pelo GM. */
public record PagamentoRequest(
        @NotNull(message = "Informe o valor.") @DecimalMin(value = "0.00", message = "Valor inválido.") BigDecimal valor,
        @NotNull(message = "Informe quantos dias o pagamento libera.") @Min(value = 1, message = "Informe pelo menos 1 dia.") Integer dias,
        @NotNull(message = "Informe a forma de pagamento.") FormaPagamento forma,
        @Size(max = 255) String observacao) {
}
