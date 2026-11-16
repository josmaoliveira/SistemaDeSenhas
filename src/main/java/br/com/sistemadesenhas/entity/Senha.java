package br.com.sistemadesenhas.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import br.com.sistemadesenhas.entity.enums.StatusSenha;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Senha retirada no totem. */
@Entity
@Table(name = "senhas")
@Getter
@Setter
@NoArgsConstructor
public class Senha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    /** Código mostrado ao cliente, ex.: CA001. */
    private String codigo;

    private LocalDateTime dataHoraGeracao;

    @Enumerated(EnumType.STRING)
    private StatusSenha status;

    // Muitas senhas para um serviço (N:1)
    @ManyToOne
    @JoinColumn(name = "servico_id")
    private Servico servico;

    // Muitas senhas para uma prioridade (N:1)
    @ManyToOne
    @JoinColumn(name = "prioridade_id")
    private Prioridade prioridade;
}
