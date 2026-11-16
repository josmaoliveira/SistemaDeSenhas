package br.com.sistemadesenhas.controller;

import br.com.sistemadesenhas.dto.CadastroEmpresaRequest;
import br.com.sistemadesenhas.dto.EmpresaResumo;
import br.com.sistemadesenhas.dto.LicencaResponse;
import br.com.sistemadesenhas.entity.Empresa;
import br.com.sistemadesenhas.security.UsuarioLogado;
import br.com.sistemadesenhas.service.EmpresaService;
import br.com.sistemadesenhas.service.LicencaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Cadastro de uma nova empresa pelo próprio cliente (precisa estar logado). */
@RestController
@RequiredArgsConstructor
public class CadastroController {

    private final EmpresaService empresaService;
    private final LicencaService licencaService;
    private final UsuarioLogado usuarioLogado;

    /** Resposta do cadastro: dados da empresa criada e da licença (com o serial e o vencimento). */
    public record CadastroResponse(EmpresaResumo empresa, LicencaResponse licenca) {
    }

    @PostMapping("/api/cadastro/empresa")
    @ResponseStatus(HttpStatus.CREATED)
    public CadastroResponse cadastrar(@RequestBody @Valid CadastroEmpresaRequest dados) {
        Empresa empresa = empresaService.cadastrar(dados, usuarioLogado.identidade());
        return new CadastroResponse(EmpresaResumo.de(empresa), licencaService.resumo(empresa));
    }
}
