package br.com.sistemadesenhas.entity.enums;

/**
 * Situação da licença, CALCULADA a partir do status, do período grátis e da data de vencimento
 * (não é salva no banco). Veja LicencaService#situacao.
 */
public enum SituacaoLicenca {
    /** Dentro dos dias grátis, ainda sem nenhum pagamento. */
    EM_TESTE,
    /** Já pagou e está dentro do vencimento. */
    ATIVA,
    /** Passou da data de vencimento. */
    VENCIDA,
    /** Bloqueada pelo GM. */
    BLOQUEADA;

    /** @return true se a empresa pode usar o sistema nesta situação */
    public boolean liberada() {
        return this == EM_TESTE || this == ATIVA;
    }
}
