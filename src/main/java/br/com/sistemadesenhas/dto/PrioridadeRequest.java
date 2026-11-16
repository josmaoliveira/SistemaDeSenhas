package br.com.sistemadesenhas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Dados para criar/editar uma prioridade. Peso maior = chamada antes. */
public record PrioridadeRequest(
        @NotBlank(message = "Informe o nome da prioridade.") @Size(max = 100) String nome,
        @NotNull(message = "Informe o peso da prioridade.") Integer peso) {
}
