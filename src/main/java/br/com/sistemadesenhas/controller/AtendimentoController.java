package br.com.sistemadesenhas.controller;

import br.com.sistemadesenhas.entity.Atendimento;
import br.com.sistemadesenhas.service.AtendimentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Tela do guichê (perfis ATENDENTE e ADMIN_EMPRESA). */
@RestController
@RequestMapping("/api/atendimentos")
@RequiredArgsConstructor
public class AtendimentoController {

    private final AtendimentoService atendimentoService;

    /** @return 200 com o atendimento, ou 204 se não houver senhas aguardando */
    @PostMapping("/chamar/{guicheId}")
    public ResponseEntity<Atendimento> chamar(@PathVariable Long guicheId) {
        Atendimento atendimento = atendimentoService.chamarProxima(guicheId);
        return atendimento == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(atendimento);
    }

    /** @return 200 com o atendimento em andamento, ou 204 se o guichê estiver livre */
    @GetMapping("/atual/{guicheId}")
    public ResponseEntity<Atendimento> atual(@PathVariable Long guicheId) {
        Atendimento atendimento = atendimentoService.atual(guicheId);
        return atendimento == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(atendimento);
    }

    @GetMapping("/recentes/{guicheId}")
    public List<Atendimento> recentes(@PathVariable Long guicheId) {
        return atendimentoService.recentes(guicheId);
    }

    @PostMapping("/rechamar/{id}")
    public Atendimento rechamar(@PathVariable Long id) {
        return atendimentoService.rechamar(id);
    }

    @PutMapping("/finalizar/{id}")
    public void finalizar(@PathVariable Long id) {
        atendimentoService.finalizar(id);
    }

    /** "Continuo neste guichê": a tela do atendente chama a cada minuto. */
    @PostMapping("/presenca/{guicheId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void presenca(@PathVariable Long guicheId) {
        atendimentoService.registrarPresenca(guicheId);
    }

    /** "Saí do guichê": chamado ao trocar de guichê ou sair do sistema. */
    @DeleteMapping("/presenca/{guicheId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sairDoGuiche(@PathVariable Long guicheId) {
        atendimentoService.encerrarPresenca(guicheId);
    }
}
