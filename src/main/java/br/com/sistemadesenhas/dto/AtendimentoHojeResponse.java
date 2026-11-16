package br.com.sistemadesenhas.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Relatório "Atendimento de hoje" do administrador da empresa.
 *
 * @param guichesLigados        guichês com a tela do atendente aberta agora
 * @param guichesQueAtenderam   guichês que atenderam pelo menos uma senha hoje
 * @param totalAtendimentos     senhas chamadas hoje (todas as do dia)
 * @param emAndamento           atendimentos acontecendo agora
 * @param tempoMedioSegundos    média dos atendimentos já finalizados hoje (null se nenhum)
 * @param guiches               detalhe de cada guichê
 */
public record AtendimentoHojeResponse(int totalGuiches, long guichesLigados, long guichesQueAtenderam,
                                      long totalAtendimentos, long emAndamento, Long tempoMedioSegundos,
                                      List<Guiche> guiches) {

    /**
     * @param ligado             tela do atendente aberta agora
     * @param atendente          quem está no guichê (null se ninguém)
     * @param atendimentos       senhas chamadas hoje neste guichê
     * @param tempoMedioSegundos média dos atendimentos finalizados hoje neste guichê (null se nenhum)
     * @param ultimoAtendimento  início do último atendimento de hoje (null se nenhum)
     */
    public record Guiche(Long id, Integer numero, String nome, String servico, boolean ligado, boolean atendendo,
                         String atendente, long atendimentos, Long tempoMedioSegundos, LocalDateTime ultimoAtendimento) {
    }
}
