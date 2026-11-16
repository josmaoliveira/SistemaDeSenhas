package br.com.sistemadesenhas.dto;

import br.com.sistemadesenhas.entity.Empresa;

/** Dados básicos da empresa para mostrar nas telas. */
public record EmpresaResumo(Long id, String nomeFantasia, String slug) {

    public static EmpresaResumo de(Empresa e) {
        return e == null ? null : new EmpresaResumo(e.getId(), e.getNomeFantasia(), e.getSlug());
    }
}
