package br.com.sistemadesenhas.dto;

import br.com.sistemadesenhas.entity.RegistroLogin;

import java.time.LocalDateTime;

/** Uma linha do histórico de logins. */
public record LoginResponse(Long id, LocalDateTime dataHora, String usuario, String email, String empresa,
                            String ip, String provedor) {

    public static LoginResponse de(RegistroLogin r) {
        return new LoginResponse(r.getId(), r.getDataHora(), r.getUsuario().getNome(), r.getUsuario().getEmail(),
                r.getEmpresa() == null ? "(GM)" : r.getEmpresa().getNomeFantasia(), r.getIp(), r.getProvedor());
    }
}
