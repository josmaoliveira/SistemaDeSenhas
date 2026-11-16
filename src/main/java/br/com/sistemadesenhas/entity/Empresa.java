package br.com.sistemadesenhas.entity;

import br.com.sistemadesenhas.entity.enums.StatusEmpresa;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Empresa cliente do sistema. Tudo (serviços, guichês, senhas, usuários...) pertence a uma empresa,
 * e cada empresa só enxerga os próprios dados.
 */
@Entity
@Table(name = "empresas")
@Getter
@Setter
@NoArgsConstructor
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String razaoSocial;
    private String nomeFantasia;

    /** CNPJ só com números (14 dígitos). */
    private String cnpj;

    /** CPF do responsável só com números (11 dígitos). */
    private String cpfResponsavel;
    private String nomeResponsavel;
    private String email;
    private String telefone;
    private boolean telefoneVerificado;

    /** Identificador curto usado nos links públicos, ex.: /index.html?empresa=padaria-central */
    private String slug;

    /** Serial XXXX-XXXX-XXXX-XXXX calculado a partir do CNPJ (veja SerialService). */
    private String serial;

    /** Aumenta quando um serial novo é gerado; entra no cálculo do serial. */
    private int versaoSerial = 1;

    /** ATIVA ou BLOQUEADA (bloqueio manual). */
    @Enumerated(EnumType.STRING)
    private StatusEmpresa status = StatusEmpresa.ATIVA;

    /** true até o primeiro pagamento. */
    private boolean emPeriodoGratis = true;

    private LocalDateTime dataCadastro;

    /** Último dia em que a empresa pode usar o sistema. */
    private LocalDate dataVencimento;

    /** Mensalidade própria desta empresa. null = paga o valor padrão (tabela configuracoes). */
    private BigDecimal valorMensal;
}
