package br.com.sistemadesenhas.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Registro de uma senha sendo atendida em um guichê. Fim nulo = atendimento em andamento. */
@Entity
@Table(name = "atendimentos")
@Getter
@Setter
@NoArgsConstructor
public class Atendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    private LocalDateTime dataHoraInicio;
    private LocalDateTime dataHoraFim;

    @OneToOne
    @JoinColumn(name = "senha_id", unique = true)
    private Senha senha;

    @ManyToOne
    @JoinColumn(name = "guiche_id")
    private Guiche guiche;

    /** Quem fez o atendimento. */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}
