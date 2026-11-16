package br.com.sistemadesenhas.controller.empresa;

import br.com.sistemadesenhas.dto.GuicheRequest;
import br.com.sistemadesenhas.dto.PrioridadeRequest;
import br.com.sistemadesenhas.dto.ServicoRequest;
import br.com.sistemadesenhas.entity.Guiche;
import br.com.sistemadesenhas.entity.Prioridade;
import br.com.sistemadesenhas.entity.Servico;
import br.com.sistemadesenhas.service.CadastrosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Serviços, prioridades e guichês da empresa do usuário logado (perfil ADMIN_EMPRESA).
 * <p>
 * Exclusões com registros ligados respondem 409 (CONFIRMAR_EXCLUSAO): a tela pergunta e
 * repete a chamada com ?confirmado=true.
 */
@RestController
@RequestMapping("/api/empresa")
@RequiredArgsConstructor
public class CadastrosController {

    private final CadastrosService cadastros;

    // ---------------- serviços
    @GetMapping("/servicos")
    public List<Servico> servicos() {
        return cadastros.listarServicos();
    }

    @PostMapping("/servicos")
    @ResponseStatus(HttpStatus.CREATED)
    public Servico criarServico(@RequestBody @Valid ServicoRequest dados) {
        return cadastros.salvarServico(null, dados);
    }

    @PutMapping("/servicos/{id}")
    public Servico editarServico(@PathVariable Long id, @RequestBody @Valid ServicoRequest dados) {
        return cadastros.salvarServico(id, dados);
    }

    @DeleteMapping("/servicos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirServico(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean confirmado) {
        cadastros.excluirServico(id, confirmado);
    }

    // ---------------- prioridades
    @GetMapping("/prioridades")
    public List<Prioridade> prioridades() {
        return cadastros.listarPrioridades();
    }

    @PostMapping("/prioridades")
    @ResponseStatus(HttpStatus.CREATED)
    public Prioridade criarPrioridade(@RequestBody @Valid PrioridadeRequest dados) {
        return cadastros.salvarPrioridade(null, dados);
    }

    @PutMapping("/prioridades/{id}")
    public Prioridade editarPrioridade(@PathVariable Long id, @RequestBody @Valid PrioridadeRequest dados) {
        return cadastros.salvarPrioridade(id, dados);
    }

    @DeleteMapping("/prioridades/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirPrioridade(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean confirmado) {
        cadastros.excluirPrioridade(id, confirmado);
    }

    // ---------------- guichês (a listagem também é liberada para ATENDENTE, veja SecurityConfig)
    @GetMapping("/guiches")
    public List<Guiche> guiches() {
        return cadastros.listarGuiches();
    }

    @PostMapping("/guiches")
    @ResponseStatus(HttpStatus.CREATED)
    public Guiche criarGuiche(@RequestBody @Valid GuicheRequest dados) {
        return cadastros.salvarGuiche(null, dados);
    }

    @PutMapping("/guiches/{id}")
    public Guiche editarGuiche(@PathVariable Long id, @RequestBody @Valid GuicheRequest dados) {
        return cadastros.salvarGuiche(id, dados);
    }

    @DeleteMapping("/guiches/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirGuiche(@PathVariable Long id) {
        cadastros.excluirGuiche(id);
    }
}
