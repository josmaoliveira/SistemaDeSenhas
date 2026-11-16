package br.com.sistemadesenhas.dto;

import br.com.sistemadesenhas.entity.enums.SituacaoLicenca;
import br.com.sistemadesenhas.entity.enums.StatusEmpresa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Todos os dados de uma empresa, para o painel do GM.
 * valorMensal é o que a empresa paga; valorMensalProprio = false quando é o valor padrão.
 */
public record EmpresaGmResponse(Long id, String razaoSocial, String nomeFantasia, String cnpj,
                                String cpfResponsavel, String nomeResponsavel, String email, String telefone,
                                boolean telefoneVerificado, String slug, String serial, StatusEmpresa status,
                                SituacaoLicenca situacao, boolean emPeriodoGratis, LocalDateTime dataCadastro,
                                LocalDate dataVencimento, long diasRestantes,
                                BigDecimal valorMensal, boolean valorMensalProprio) {
}
