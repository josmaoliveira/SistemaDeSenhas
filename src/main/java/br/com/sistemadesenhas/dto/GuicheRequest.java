package br.com.sistemadesenhas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Dados para criar/editar um guichê. */
public record GuicheRequest(
        @NotBlank(message = "Informe o nome do guichê.") @Size(max = 100) String nome,
        Integer numero,
        @Size(max = 100) String setor,
        @NotNull(message = "Escolha o serviço do guichê.") Long servicoId) {
}
