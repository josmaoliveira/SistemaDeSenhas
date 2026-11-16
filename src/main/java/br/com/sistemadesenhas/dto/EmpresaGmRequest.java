package br.com.sistemadesenhas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dados de empresa que o GM pode corrigir. */
public record EmpresaGmRequest(
        @NotBlank(message = "Informe a razão social.") @Size(max = 150) String razaoSocial,
        @NotBlank(message = "Informe o nome fantasia.") @Size(max = 150) String nomeFantasia,
        @NotBlank(message = "Informe o CNPJ.") String cnpj,
        @NotBlank(message = "Informe o CPF do responsável.") String cpfResponsavel,
        @NotBlank(message = "Informe o nome do responsável.") @Size(max = 150) String nomeResponsavel,
        @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") String email,
        String telefone) {
}
