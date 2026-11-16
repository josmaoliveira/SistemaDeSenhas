package br.com.sistemadesenhas.dto;

import br.com.sistemadesenhas.entity.enums.StatusEmpresa;
import jakarta.validation.constraints.NotNull;

public record StatusEmpresaRequest(@NotNull(message = "Informe o status.") StatusEmpresa status) {
}
