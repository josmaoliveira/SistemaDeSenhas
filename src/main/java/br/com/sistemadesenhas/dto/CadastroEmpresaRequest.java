package br.com.sistemadesenhas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Formulário de cadastro de uma nova empresa (feito pelo próprio cliente). */
public record CadastroEmpresaRequest(
        @NotBlank(message = "Informe a razão social.") @Size(max = 150) String razaoSocial,
        @NotBlank(message = "Informe o nome fantasia.") @Size(max = 150) String nomeFantasia,
        @NotBlank(message = "Informe o CNPJ.") String cnpj,
        @NotBlank(message = "Informe o CPF do responsável.") String cpfResponsavel,
        @NotBlank(message = "Informe o nome do responsável.") @Size(max = 150) String nomeResponsavel,
        String telefone) {
}
