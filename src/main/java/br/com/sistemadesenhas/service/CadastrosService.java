package br.com.sistemadesenhas.service;

import br.com.sistemadesenhas.dto.GuicheRequest;
import br.com.sistemadesenhas.dto.PrioridadeRequest;
import br.com.sistemadesenhas.dto.ServicoRequest;
import br.com.sistemadesenhas.entity.Guiche;
import br.com.sistemadesenhas.entity.Prioridade;
import br.com.sistemadesenhas.entity.Servico;
import br.com.sistemadesenhas.exception.ErroNegocio;
import br.com.sistemadesenhas.repository.AtendimentoRepository;
import br.com.sistemadesenhas.repository.GuicheRepository;
import br.com.sistemadesenhas.repository.PrioridadeRepository;
import br.com.sistemadesenhas.repository.SenhaRepository;
import br.com.sistemadesenhas.repository.ServicoRepository;
import br.com.sistemadesenhas.security.UsuarioLogado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Cadastros da empresa: serviços, prioridades e guichês.
 * <p>
 * ISOLAMENTO: todo método usa {@code usuarioLogado.empresaId()}. Ao criar, a empresa é sempre a do
 * usuário logado; ao buscar/editar/excluir, procura o registro DENTRO da empresa. Se alguém mandar o id
 * de um registro de outra empresa, recebe "não encontrado".
 */
@Service
@RequiredArgsConstructor
public class CadastrosService {

    private final ServicoRepository servicoRepository;
    private final PrioridadeRepository prioridadeRepository;
    private final GuicheRepository guicheRepository;
    private final SenhaRepository senhaRepository;
    private final AtendimentoRepository atendimentoRepository;
    private final UsuarioLogado usuarioLogado;

    // ------------------------------------------------------------------ serviços

    public List<Servico> listarServicos() {
        return servicoRepository.findByEmpresaIdOrderByNomeAsc(usuarioLogado.empresaId());
    }

    @Transactional
    public Servico salvarServico(Long id, ServicoRequest dados) {
        Servico servico = id == null ? novoServico() : buscarServico(id);
        servico.setNome(dados.nome().trim());
        servico.setDescricao(dados.descricao());
        servico.setSigla(dados.sigla().trim().toUpperCase());
        return servicoRepository.save(servico);
    }

    /**
     * Exclui o serviço. Se houver guichês, senhas ou atendimentos ligados, só exclui com confirmado=true
     * (o banco apaga tudo junto, pelo ON DELETE CASCADE).
     */
    @Transactional
    public void excluirServico(Long id, boolean confirmado) {
        Servico servico = buscarServico(id);
        long senhas = senhaRepository.countByServicoId(id);
        long guiches = guicheRepository.countByServicoId(id);
        long atendimentos = atendimentoRepository.countByGuicheServicoId(id);
        if ((senhas + guiches + atendimentos) > 0 && !confirmado) {
            throw ErroNegocio.conflito("CONFIRMAR_EXCLUSAO", "Este serviço possui " + senhas + " senha(s), "
                    + guiches + " guichê(s) e " + atendimentos + " atendimento(s). Deseja excluir TUDO permanentemente?");
        }
        servicoRepository.delete(servico);
    }

    private Servico novoServico() {
        Servico s = new Servico();
        s.setEmpresa(usuarioLogado.empresa());
        return s;
    }

    private Servico buscarServico(Long id) {
        return servicoRepository.findByIdAndEmpresaId(id, usuarioLogado.empresaId())
                .orElseThrow(() -> ErroNegocio.naoEncontrado("Serviço"));
    }

    // ------------------------------------------------------------------ prioridades

    public List<Prioridade> listarPrioridades() {
        return prioridadeRepository.findByEmpresaIdOrderByPesoDescNomeAsc(usuarioLogado.empresaId());
    }

    @Transactional
    public Prioridade salvarPrioridade(Long id, PrioridadeRequest dados) {
        Prioridade prioridade;
        if (id == null) {
            prioridade = new Prioridade();
            prioridade.setEmpresa(usuarioLogado.empresa());
        } else {
            prioridade = buscarPrioridade(id);
        }
        prioridade.setNome(dados.nome().trim());
        prioridade.setPeso(dados.peso());
        return prioridadeRepository.save(prioridade);
    }

    @Transactional
    public void excluirPrioridade(Long id, boolean confirmado) {
        Prioridade prioridade = buscarPrioridade(id);
        long senhas = senhaRepository.countByPrioridadeId(id);
        if (senhas > 0 && !confirmado) {
            throw ErroNegocio.conflito("CONFIRMAR_EXCLUSAO",
                    "Existem " + senhas + " senha(s) com esta prioridade. Deseja excluir mesmo assim?");
        }
        prioridadeRepository.delete(prioridade);
    }

    private Prioridade buscarPrioridade(Long id) {
        return prioridadeRepository.findByIdAndEmpresaId(id, usuarioLogado.empresaId())
                .orElseThrow(() -> ErroNegocio.naoEncontrado("Prioridade"));
    }

    // ------------------------------------------------------------------ guichês

    public List<Guiche> listarGuiches() {
        return guicheRepository.findByEmpresaIdOrderByNumeroAscNomeAsc(usuarioLogado.empresaId());
    }

    @Transactional
    public Guiche salvarGuiche(Long id, GuicheRequest dados) {
        Guiche guiche;
        if (id == null) {
            guiche = new Guiche();
            guiche.setEmpresa(usuarioLogado.empresa());
        } else {
            guiche = buscarGuiche(id);
        }
        guiche.setNome(dados.nome().trim());
        guiche.setNumero(dados.numero());
        guiche.setSetor(dados.setor());
        // O serviço também precisa ser da mesma empresa
        guiche.setServico(buscarServico(dados.servicoId()));
        return guicheRepository.save(guiche);
    }

    @Transactional
    public void excluirGuiche(Long id) {
        guicheRepository.delete(buscarGuiche(id));
    }

    public Guiche buscarGuiche(Long id) {
        return guicheRepository.findByIdAndEmpresaId(id, usuarioLogado.empresaId())
                .orElseThrow(() -> ErroNegocio.naoEncontrado("Guichê"));
    }
}
