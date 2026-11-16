package br.com.sistemadesenhas.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import br.com.sistemadesenhas.entity.enums.Perfil;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Pessoa que faz login no sistema.
 * <p>
 * A senha NÃO fica aqui: quem cuida do login (e-mail/senha, Google, SMS) é o Firebase.
 * Aqui guardamos quem a pessoa é dentro do sistema: nome, perfil e empresa.
 * O campo {@code firebaseUid} liga este registro à conta do Firebase e é preenchido no primeiro login.
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Empresa do usuário. É null apenas para o GM, que não pertence a nenhuma empresa. */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @JsonIgnore
    private String firebaseUid;

    private String nome;
    private String email;

    /** Telefone no formato internacional, ex.: +5583999990000 (usado no login por SMS). */
    private String telefone;

    @Enumerated(EnumType.STRING)
    private Perfil perfil;

    private boolean ativo = true;
    private LocalDateTime dataCadastro;
    private LocalDateTime ultimoLogin;

    /** @return true se a pessoa já fez o primeiro login (já tem conta no Firebase ligada) */
    public boolean isPrimeiroAcessoFeito() {
        return firebaseUid != null;
    }
}
