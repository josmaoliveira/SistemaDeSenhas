package br.com.sistemadesenhas.controller.empresa;

import br.com.sistemadesenhas.dto.AtendimentoHojeResponse;
import br.com.sistemadesenhas.service.RelatorioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Relatórios da empresa do usuário logado (perfil ADMIN_EMPRESA, veja SecurityConfig). */
@RestController
@RequestMapping("/api/empresa/relatorios")
@RequiredArgsConstructor
public class RelatorioController {

    private final RelatorioService relatorioService;

    /** Guichês ligados, atendimentos por guichê, total do dia e tempo médio. */
    @GetMapping("/atendimento-hoje")
    public AtendimentoHojeResponse atendimentoDeHoje() {
        return relatorioService.atendimentoDeHoje();
    }
}
