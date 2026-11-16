package br.com.sistemadesenhas.dto;

import br.com.sistemadesenhas.entity.enums.SituacaoLicenca;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Situação da licença de uma empresa. */
public record LicencaResponse(SituacaoLicenca situacao, boolean liberada, LocalDate dataVencimento,
                              long diasRestantes, boolean emPeriodoGratis, String serial, String cnpj,
                              BigDecimal valorMensal) {
}
