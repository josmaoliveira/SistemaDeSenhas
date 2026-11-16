package br.com.sistemadesenhas.dto;

import br.com.sistemadesenhas.entity.enums.FormaPagamento;
import br.com.sistemadesenhas.entity.Pagamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PagamentoResponse(Long id, Long empresaId, String empresa, BigDecimal valor, LocalDateTime dataPagamento,
                                int diasAdicionados, FormaPagamento forma, String observacao,
                                LocalDate vencimentoAnterior, LocalDate vencimentoNovo, String registradoPor) {

    public static PagamentoResponse de(Pagamento p) {
        return new PagamentoResponse(p.getId(), p.getEmpresa().getId(), p.getEmpresa().getNomeFantasia(),
                p.getValor(), p.getDataPagamento(), p.getDiasAdicionados(), p.getForma(), p.getObservacao(),
                p.getVencimentoAnterior(), p.getVencimentoNovo(),
                p.getRegistradoPor() == null ? null : p.getRegistradoPor().getNome());
    }
}
