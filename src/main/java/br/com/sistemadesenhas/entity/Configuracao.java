package br.com.sistemadesenhas.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Configurações gerais do sistema, alteradas pelo GM. A tabela tem uma linha só (id = 1). */
@Entity
@Table(name = "configuracoes")
@Getter
@Setter
@NoArgsConstructor
public class Configuracao {

    /** Id da única linha da tabela. */
    public static final int UNICA = 1;

    @Id
    private Integer id;

    /** Mensalidade cobrada das empresas que não têm valor próprio. */
    private BigDecimal valorMensalPadrao;
}
