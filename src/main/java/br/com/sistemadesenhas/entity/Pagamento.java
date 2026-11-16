package br.com.sistemadesenhas.entity;

import br.com.sistemadesenhas.entity.enums.FormaPagamento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Pagamento de uma empresa. Cada pagamento adiciona dias à data de vencimento. */
@Entity
@Table(name = "pagamentos")
@Getter
@Setter
@NoArgsConstructor
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    private BigDecimal valor;
    private LocalDateTime dataPagamento;
    private int diasAdicionados;

    @Enumerated(EnumType.STRING)
    private FormaPagamento forma;

    private String observacao;
    private LocalDate vencimentoAnterior;
    private LocalDate vencimentoNovo;

    /** GM que registrou o pagamento (null quando vier automático, ex.: Pix). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por_id")
    private Usuario registradoPor;
}
