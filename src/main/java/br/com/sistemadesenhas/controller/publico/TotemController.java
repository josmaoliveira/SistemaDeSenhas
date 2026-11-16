package br.com.sistemadesenhas.controller.publico;

import br.com.sistemadesenhas.dto.EmpresaPublicaResponse;
import br.com.sistemadesenhas.dto.PainelItem;
import br.com.sistemadesenhas.dto.SenhaGeradaResponse;
import br.com.sistemadesenhas.entity.Prioridade;
import br.com.sistemadesenhas.entity.Servico;
import br.com.sistemadesenhas.service.SenhaService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Rotas SEM login usadas pelo totem (index.html) e pelo painel da TV (painel.html).
 * O {slug} identifica a empresa, ex.: /api/publico/padaria-central/servicos
 */
@RestController
@RequestMapping("/api/publico/{slug}")
@RequiredArgsConstructor
public class TotemController {

    private final SenhaService senhaService;

    /** Corpo do POST de nova senha. */
    public record NovaSenhaTotem(@NotNull Long servicoId, @NotNull Long prioridadeId) {
    }

    @GetMapping
    public EmpresaPublicaResponse empresa(@PathVariable String slug) {
        return senhaService.empresaPublica(slug);
    }

    @GetMapping("/servicos")
    public List<Servico> servicos(@PathVariable String slug) {
        return senhaService.servicos(slug);
    }

    @GetMapping("/prioridades")
    public List<Prioridade> prioridades(@PathVariable String slug) {
        return senhaService.prioridades(slug);
    }

    @PostMapping("/senhas")
    @ResponseStatus(HttpStatus.CREATED)
    public SenhaGeradaResponse gerarSenha(@PathVariable String slug, @RequestBody @jakarta.validation.Valid NovaSenhaTotem dados) {
        return senhaService.gerar(slug, dados.servicoId(), dados.prioridadeId());
    }

    @PostMapping("/senhas/{id}/imprimir")
    public void imprimir(@PathVariable String slug, @PathVariable Long id) {
        senhaService.imprimir(slug, id);
    }

    @GetMapping("/painel")
    public List<PainelItem> painel(@PathVariable String slug) {
        return senhaService.painel(slug);
    }
}
