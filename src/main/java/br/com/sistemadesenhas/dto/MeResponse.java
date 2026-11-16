package br.com.sistemadesenhas.dto;

import br.com.sistemadesenhas.entity.enums.Perfil;

/** Resposta de "quem sou eu": usada pelas telas para saber o perfil e para onde ir. */
public record MeResponse(Long id, String nome, String email, Perfil perfil,
                         EmpresaResumo empresa, LicencaResponse licenca) {
}
