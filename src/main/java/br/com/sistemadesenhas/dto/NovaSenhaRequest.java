package br.com.sistemadesenhas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Nova senha definida pelo GM para um usuário (exige credenciais do Firebase configuradas). */
public record NovaSenhaRequest(
        @NotBlank(message = "Informe a nova senha.") @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres.") String novaSenha) {
}
