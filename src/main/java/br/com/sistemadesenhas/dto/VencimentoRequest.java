package br.com.sistemadesenhas.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record VencimentoRequest(@NotNull(message = "Informe a data de vencimento.") LocalDate dataVencimento) {
}
