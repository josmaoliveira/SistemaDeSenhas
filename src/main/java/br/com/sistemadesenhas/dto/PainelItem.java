package br.com.sistemadesenhas.dto;

import java.time.LocalDateTime;

/** Uma senha em atendimento, como aparece no painel da TV. */
public record PainelItem(String codigo, String guiche, LocalDateTime inicio) {
}
