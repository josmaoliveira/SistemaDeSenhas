package br.com.sistemadesenhas.controller.empresa;

import br.com.sistemadesenhas.dto.LicencaResponse;
import br.com.sistemadesenhas.security.UsuarioLogado;
import br.com.sistemadesenhas.service.LicencaService;
import br.com.sistemadesenhas.service.SenhaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Licença e operações gerais da empresa do usuário logado (perfil ADMIN_EMPRESA). */
@RestController
@RequestMapping("/api/empresa")
@RequiredArgsConstructor
public class MinhaEmpresaController {

    private final UsuarioLogado usuarioLogado;
    private final LicencaService licencaService;
    private final SenhaService senhaService;

    /** Situação da licença. Funciona mesmo com a licença vencida (veja WebConfig). */
    @GetMapping("/licenca")
    public LicencaResponse licenca() {
        return licencaService.resumo(usuarioLogado.empresa());
    }

    /** Fim do dia: apaga as senhas e atendimentos da empresa. */
    @DeleteMapping("/senhas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetarFila() {
        senhaService.resetarFila();
    }
}
