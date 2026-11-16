package br.com.sistemadesenhas.entity.enums;

/** Situação de uma senha dentro da fila. */
public enum StatusSenha {
    /** Cliente tirou a senha e está esperando ser chamado. */
    AGUARDANDO,
    /** Senha chamada por um guichê (atendimento em andamento). */
    CHAMADA,
    /** Atendimento encerrado. */
    FINALIZADA
}
