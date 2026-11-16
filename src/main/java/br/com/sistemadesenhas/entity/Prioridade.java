package br.com.sistemadesenhas.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Prioridade da senha. Quanto MAIOR o peso, antes a senha é chamada. */
@Entity
@Table(name = "prioridades")
@Getter
@Setter
@NoArgsConstructor
public class Prioridade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    private String nome;
    private Integer peso;
}
