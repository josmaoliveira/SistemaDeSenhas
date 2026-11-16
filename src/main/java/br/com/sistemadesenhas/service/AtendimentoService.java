package br.com.sistemadesenhas.service;

import br.com.sistemadesenhas.entity.Atendimento;
import br.com.sistemadesenhas.entity.enums.StatusSenha;
import br.com.sistemadesenhas.entity.Guiche;
import br.com.sistemadesenhas.entity.Senha;
import br.com.sistemadesenhas.exception.ErroNegocio;
import br.com.sistemadesenhas.repository.AtendimentoRepository;
import br.com.sistemadesenhas.repository.GuicheRepository;
import br.com.sistemadesenhas.repository.SenhaRepository;
import br.com.sistemadesenhas.security.UsuarioLogado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Operação do guichê: chamar, rechamar e finalizar senhas.
 * Todo guichê e atendimento é buscado DENTRO da empresa do usuário logado.
 */
@Service
@RequiredArgsConstructor
public class AtendimentoService {

    private final SenhaRepository senhaRepository;
    private final GuicheRepository guicheRepository;
    private final AtendimentoRepository atendimentoRepository;
    private final NextSenhaStrategy nextSenhaStrategy;
    private final UsuarioLogado usuarioLogado;

    /**
     * Chama a próxima senha para o guichê.
     * <ol>
     *   <li>Se o guichê tinha um atendimento aberto, finaliza;</li>
     *   <li>Pede à estratégia ({@link NextSenhaStrategy}) a próxima senha do serviço do guichê;</li>
     *   <li>Marca a senha como CHAMADA e abre um novo atendimento.</li>
     * </ol>
     *
     * @return o novo atendimento, ou null se a fila estiver vazia
     */
    @Transactional
    public Atendimento chamarProxima(Long guicheId) {
        Guiche guiche = buscarGuiche(guicheId);

        atendimentoRepository.findByGuicheIdAndDataHoraFimIsNull(guicheId).ifPresent(this::encerrar);

        List<Senha> fila = nextSenhaStrategy.encontrarProxima(guiche.getServico());
        if (fila.isEmpty()) {
            guiche.setOcupado(false);
            guicheRepository.save(guiche);
            return null;
        }

        Senha proxima = fila.get(0);
        proxima.setStatus(StatusSenha.CHAMADA);
        senhaRepository.saveAndFlush(proxima);

        guiche.setOcupado(true);
        guicheRepository.save(guiche);

        Atendimento atendimento = new Atendimento();
        atendimento.setEmpresa(guiche.getEmpresa());
        atendimento.setSenha(proxima);
        atendimento.setGuiche(guiche);
        atendimento.setUsuario(usuarioLogado.usuario());
        atendimento.setDataHoraInicio(LocalDateTime.now());
        return atendimentoRepository.save(atendimento);
    }

    /** @return atendimento em andamento no guichê, ou null */
    public Atendimento atual(Long guicheId) {
        buscarGuiche(guicheId);
        return atendimentoRepository.findByGuicheIdAndDataHoraFimIsNull(guicheId).orElse(null);
    }

    /** Últimos atendimentos do guichê (histórico da tela do atendente). */
    public List<Atendimento> recentes(Long guicheId) {
        buscarGuiche(guicheId);
        return atendimentoRepository.findTop10ByGuicheIdOrderByDataHoraInicioDesc(guicheId);
    }

    /**
     * Chama de novo uma senha (o cliente não apareceu da primeira vez).
     * O atendimento volta a ficar "em andamento" e reaparece no painel.
     */
    @Transactional
    public Atendimento rechamar(Long atendimentoId) {
        Atendimento atendimento = buscarAtendimento(atendimentoId);

        // Não pode rechamar uma senha antiga com outra em andamento no mesmo guichê
        atendimentoRepository.findByGuicheIdAndDataHoraFimIsNull(atendimento.getGuiche().getId())
                .filter(aberto -> !aberto.getId().equals(atendimentoId))
                .ifPresent(aberto -> {
                    throw ErroNegocio.conflito("ATENDIMENTO_ABERTO",
                            "Finalize o atendimento atual antes de rechamar uma senha antiga.");
                });

        Senha senha = atendimento.getSenha();
        senha.setStatus(StatusSenha.CHAMADA);
        senhaRepository.save(senha);

        // Tirar a data de fim faz o atendimento voltar a ser "ativo" (aparece no painel de novo)
        atendimento.setDataHoraFim(null);
        atendimento.setDataHoraInicio(LocalDateTime.now());

        Guiche guiche = atendimento.getGuiche();
        guiche.setOcupado(true);
        guicheRepository.save(guiche);

        return atendimentoRepository.save(atendimento);
    }

    @Transactional
    public void finalizar(Long atendimentoId) {
        encerrar(buscarAtendimento(atendimentoId));
    }

    /** Fecha o atendimento, finaliza a senha e libera o guichê. */
    private void encerrar(Atendimento atendimento) {
        atendimento.setDataHoraFim(LocalDateTime.now());
        atendimento.getSenha().setStatus(StatusSenha.FINALIZADA);
        atendimento.getGuiche().setOcupado(false);
        senhaRepository.save(atendimento.getSenha());
        guicheRepository.save(atendimento.getGuiche());
        atendimentoRepository.save(atendimento);
    }

    // ------------------------------------------------------------------ presença no guichê

    /**
     * A tela do atendente chama a cada minuto enquanto está aberta: marca o guichê como "ligado"
     * e guarda quem está nele. Usado no relatório "Atendimento de hoje" do administrador.
     */
    @Transactional
    public void registrarPresenca(Long guicheId) {
        Guiche guiche = buscarGuiche(guicheId);
        guiche.setUltimaPresenca(LocalDateTime.now());
        guiche.setUsuarioPresente(usuarioLogado.usuario());
        guicheRepository.save(guiche);
    }

    /** O atendente saiu ou trocou de guichê: o guichê aparece como desligado na hora. */
    @Transactional
    public void encerrarPresenca(Long guicheId) {
        Guiche guiche = buscarGuiche(guicheId);
        guiche.setUltimaPresenca(null);
        guiche.setUsuarioPresente(null);
        guicheRepository.save(guiche);
    }

    private Guiche buscarGuiche(Long id) {
        return guicheRepository.findByIdAndEmpresaId(id, usuarioLogado.empresaId())
                .orElseThrow(() -> ErroNegocio.naoEncontrado("Guichê"));
    }

    private Atendimento buscarAtendimento(Long id) {
        return atendimentoRepository.findByIdAndEmpresaId(id, usuarioLogado.empresaId())
                .orElseThrow(() -> ErroNegocio.naoEncontrado("Atendimento"));
    }
}
