package br.com.sistemadesenhas.dto;

import java.math.BigDecimal;

/** Números do painel inicial do GM. */
public record ResumoGmResponse(long totalEmpresas, long emTeste, long ativas, long vencidas, long bloqueadas,
                               long vencendoEm7Dias, long totalUsuarios, long loginsHoje, BigDecimal recebidoNoMes) {
}
