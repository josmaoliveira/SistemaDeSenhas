package br.com.sistemadesenhas.service;

import br.com.sistemadesenhas.dto.AtendimentoHojeResponse;
import br.com.sistemadesenhas.entity.Atendimento;
import br.com.sistemadesenhas.entity.Guiche;
import br.com.sistemadesenhas.repository.AtendimentoRepository;
import br.com.sistemadesenhas.repository.GuicheRepository;
import br.com.sistemadesenhas.security.UsuarioLogado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Números do atendimento da empresa do usuário logado, para o administrador acompanhar o dia.
 */
@Service
@RequiredArgsConstructor
public class RelatorioService {

    /**
     * A tela do atendente avisa a cada 1 minuto. Sem aviso por mais tempo que isto,
     * o guichê é considerado desligado (tela fechada, computador desligado, sem internet...).
     */
    static final Duration TOLERANCIA_PRESENCA = Duration.ofMinutes(2);

    private final GuicheRepository guicheRepository;
    private final AtendimentoRepository atendimentoRepository;
    private final UsuarioLogado usuarioLogado;

    /**
     * Monta o resumo do dia: guichês ligados, quantos atenderam, quantidade por guichê,
     * total do dia e tempo médio de atendimento.
     * <p>
     * "Hoje" começa à meia-noite (horário do servidor). O "Fim do dia" da gestão apaga os
     * atendimentos, então o relatório também zera depois dele.
     */
    @Transactional(readOnly = true)
    public AtendimentoHojeResponse atendimentoDeHoje() {
        Long empresaId = usuarioLogado.empresaId();
        LocalDateTime agora = LocalDateTime.now();
        List<Guiche> guiches = guicheRepository.findByEmpresaIdOrderByNumeroAscNomeAsc(empresaId);
        List<Atendimento> doDia = atendimentoRepository.iniciadosDesde(empresaId, LocalDate.now().atStartOfDay());
        Map<Long, List<Atendimento>> porGuiche = doDia.stream()
                .collect(Collectors.groupingBy(a -> a.getGuiche().getId()));

        List<AtendimentoHojeResponse.Guiche> linhas = guiches.stream().map(g -> {
            List<Atendimento> doGuiche = porGuiche.getOrDefault(g.getId(), List.of());
            boolean ligado = ligado(g, agora);
            return new AtendimentoHojeResponse.Guiche(g.getId(), g.getNumero(), g.getNome(), g.getServico().getNome(),
                    ligado, g.isOcupado(),
                    ligado && g.getUsuarioPresente() != null ? g.getUsuarioPresente().getNome() : null,
                    doGuiche.size(), tempoMedioSegundos(doGuiche),
                    doGuiche.stream().map(Atendimento::getDataHoraInicio).max(Comparator.naturalOrder()).orElse(null));
        }).toList();

        return new AtendimentoHojeResponse(
                guiches.size(),
                linhas.stream().filter(AtendimentoHojeResponse.Guiche::ligado).count(),
                linhas.stream().filter(l -> l.atendimentos() > 0).count(),
                doDia.size(),
                doDia.stream().filter(a -> a.getDataHoraFim() == null).count(),
                tempoMedioSegundos(doDia),
                linhas);
    }

    /** Ligado = a tela do atendente deu sinal de vida há pouco tempo. */
    private boolean ligado(Guiche guiche, LocalDateTime agora) {
        return guiche.getUltimaPresenca() != null
                && guiche.getUltimaPresenca().isAfter(agora.minus(TOLERANCIA_PRESENCA));
    }

    /**
     * Média, em segundos, do início ao fim dos atendimentos já finalizados.
     * Os que ainda estão em andamento ficam de fora (ainda não têm duração).
     *
     * @return null quando não há nenhum atendimento finalizado
     */
    private Long tempoMedioSegundos(Collection<Atendimento> atendimentos) {
        List<Long> duracoes = atendimentos.stream()
                .filter(a -> a.getDataHoraFim() != null)
                .map(a -> Duration.between(a.getDataHoraInicio(), a.getDataHoraFim()).toSeconds())
                .toList();
        if (duracoes.isEmpty()) {
            return null;
        }
        return Math.round(duracoes.stream().mapToLong(Long::longValue).average().orElse(0));
    }
}
