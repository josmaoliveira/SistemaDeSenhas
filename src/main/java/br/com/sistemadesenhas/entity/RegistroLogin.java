package br.com.sistemadesenhas.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Uma linha por login realizado. O GM consulta esse histórico no painel geral. */
@Entity
@Table(name = "registros_login")
@Getter
@Setter
@NoArgsConstructor
public class RegistroLogin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    private LocalDateTime dataHora;
    private String ip;

    /** Como entrou: password (e-mail/senha), google.com ou phone (SMS). */
    private String provedor;
}
