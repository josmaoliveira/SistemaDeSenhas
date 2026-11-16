package br.com.sistemadesenhas.dto;

import br.com.sistemadesenhas.entity.enums.Perfil;
import br.com.sistemadesenhas.entity.Usuario;

import java.time.LocalDateTime;

/** Dados de um usuário para as listas (equipe da empresa). */
public record UsuarioResponse(Long id, String nome, String email, String telefone, Perfil perfil, boolean ativo,
                              boolean primeiroAcessoFeito, LocalDateTime dataCadastro, LocalDateTime ultimoLogin,
                              Long empresaId, String empresa) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getTelefone(), u.getPerfil(), u.isAtivo(),
                u.isPrimeiroAcessoFeito(), u.getDataCadastro(), u.getUltimoLogin(),
                u.getEmpresa() == null ? null : u.getEmpresa().getId(),
                u.getEmpresa() == null ? null : u.getEmpresa().getNomeFantasia());
    }
}
