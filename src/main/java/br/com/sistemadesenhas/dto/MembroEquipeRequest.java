package br.com.sistemadesenhas.dto;

import br.com.sistemadesenhas.entity.enums.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados de um membro da equipe (cadastrado pelo administrador da empresa).
 * A pessoa depois faz o "primeiro acesso" na tela de login com este e-mail.
 */
public record MembroEquipeRequest(
        @NotBlank(message = "Informe o nome.") @Size(max = 150) String nome,
        @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") String email,
        String telefone,
        @NotNull(message = "Escolha o perfil.") Perfil perfil,
        Boolean ativo) {
}
