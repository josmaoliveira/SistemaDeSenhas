package br.com.sistemadesenhas.entity.enums;

/** Forma como um pagamento foi feito. PIX será automático na etapa 5 (Mercado Pago). */
public enum FormaPagamento {
    MANUAL,
    PIX,
    BOLETO,
    CARTAO
}
