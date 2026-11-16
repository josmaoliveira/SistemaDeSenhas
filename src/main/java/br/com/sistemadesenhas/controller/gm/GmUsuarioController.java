package br.com.sistemadesenhas.controller.gm;

import br.com.sistemadesenhas.dto.*;
import br.com.sistemadesenhas.repository.RegistroLoginRepository;
import br.com.sistemadesenhas.service.GmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Painel do GM: resumo, usuários de todas as empresas e histórico de logins. */
@RestController
@RequestMapping("/api/gm")
@RequiredArgsConstructor
public class GmUsuarioController {

    private final GmService gmService;
    private final RegistroLoginRepository registroLoginRepository;

    @GetMapping("/resumo")
    public ResumoGmResponse resumo() {
        return gmService.resumo();
    }

    /** Diz se a troca direta de senha está disponível (depende da estratégia do GerenciadorContas). */
    @GetMapping("/capacidades")
    public Map<String, Object> capacidades() {
        return gmService.capacidades();
    }

    @GetMapping("/usuarios")
    public List<UsuarioResponse> usuarios(@RequestParam(required = false) Long empresaId) {
        return gmService.listarUsuarios(empresaId).stream().map(UsuarioResponse::de).toList();
    }

    @PutMapping("/usuarios/{id}")
    public UsuarioResponse atualizar(@PathVariable Long id, @RequestBody @Valid MembroEquipeRequest dados) {
        return UsuarioResponse.de(gmService.atualizarUsuario(id, dados));
    }

    @DeleteMapping("/usuarios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        gmService.excluirUsuario(id);
    }

    @PostMapping("/usuarios/{id}/redefinir-senha")
    public void enviarRedefinicao(@PathVariable Long id) {
        gmService.enviarRedefinicaoSenha(id);
    }

    @PutMapping("/usuarios/{id}/senha")
    public void definirSenha(@PathVariable Long id, @RequestBody @Valid NovaSenhaRequest dados) {
        gmService.definirSenha(id, dados.novaSenha());
    }

    /** Últimos 300 logins (opcionalmente de uma empresa). */
    @GetMapping("/logins")
    public List<LoginResponse> logins(@RequestParam(required = false) Long empresaId) {
        return registroLoginRepository.ultimos(empresaId, PageRequest.of(0, 300)).stream().map(LoginResponse::de).toList();
    }
}
