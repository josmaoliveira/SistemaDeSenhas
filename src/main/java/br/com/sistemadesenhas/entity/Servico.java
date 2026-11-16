package br.com.sistemadesenhas.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tipo de atendimento oferecido pela empresa (ex.: Caixa, Atendimento Geral). */
@Entity
@Table(name = "servicos")
@Getter
@Setter
@NoArgsConstructor
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Empresa dona do serviço. @JsonIgnore: nunca devolvemos dados da empresa junto. */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    private String nome;
    private String descricao;

    /** As 2 primeiras letras viram o prefixo da senha (Caixa -> CA001). */
    private String sigla;
}
