package br.com.sistemadesenhas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dados para criar/editar um serviço. A empresa NÃO vem aqui: é sempre a do usuário logado. */
public record ServicoRequest(
        @NotBlank(message = "Informe o nome do serviço.") @Size(max = 100) String nome,
        @Size(max = 255) String descricao,
        @NotBlank(message = "Informe a sigla do serviço.") @Size(max = 10, message = "A sigla deve ter até 10 letras.") String sigla) {
}
