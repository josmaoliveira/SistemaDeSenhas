package br.com.sistemadesenhas.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Posto de atendimento. Cada guichê atende um único serviço. */
@Entity
@Table(name = "guiches")
@Getter
@Setter
@NoArgsConstructor
public class Guiche {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    private Integer numero;
    private String nome;
    private String setor;

    /** true enquanto o guichê tem um atendimento em andamento. */
    private boolean ocupado;

    @ManyToOne
    @JoinColumn(name = "servico_id")
    private Servico servico;

    /** Último aviso da tela do atendente de que o guichê continua aberto (veja V6__presenca_guiche.sql). */
    @JsonIgnore
    private LocalDateTime ultimaPresenca;

    /** Quem está no guichê agora (null quando ninguém). */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_presente_id")
    private Usuario usuarioPresente;
}
