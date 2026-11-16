package br.com.sistemadesenhas.dto;

import java.time.LocalDateTime;

/** Senha que acabou de ser retirada no totem. */
public record SenhaGeradaResponse(Long id, String codigo, String servico, LocalDateTime dataHora) {
}
