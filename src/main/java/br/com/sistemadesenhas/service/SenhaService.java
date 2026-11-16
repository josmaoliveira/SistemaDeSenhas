package br.com.sistemadesenhas.service;

import br.com.sistemadesenhas.dto.EmpresaPublicaResponse;
import br.com.sistemadesenhas.dto.PainelItem;
import br.com.sistemadesenhas.dto.SenhaGeradaResponse;
import br.com.sistemadesenhas.entity.Empresa;
import br.com.sistemadesenhas.entity.Prioridade;
import br.com.sistemadesenhas.entity.Senha;
import br.com.sistemadesenhas.entity.Servico;
import br.com.sistemadesenhas.exception.ErroNegocio;
import br.com.sistemadesenhas.repository.AtendimentoRepository;
import br.com.sistemadesenhas.repository.EmpresaRepository;
import br.com.sistemadesenhas.repository.PrioridadeRepository;
import br.com.sistemadesenhas.repository.SenhaRepository;
import br.com.sistemadesenhas.repository.ServicoRepository;
import br.com.sistemadesenhas.security.UsuarioLogado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Regras das telas PÚBLICAS (totem e painel) e do reset da fila.
 * <p>
 * Totem e painel não têm login: a empresa é identificada pelo "slug" que vem no link
 * (ex.: /index.html?empresa=padaria-central). Mesmo assim, tudo é filtrado pela empresa do slug,
 * e nada funciona se a licença da empresa estiver vencida ou bloqueada.
 */
@Service
@RequiredArgsConstructor
public class SenhaService {

    private final EmpresaRepository empresaRepository;
    private final ServicoRepository servicoRepository;
    private final PrioridadeRepository prioridadeRepository;
    private final SenhaRepository senhaRepository;
    private final AtendimentoRepository atendimentoRepository;
    private final SenhaFactory senhaFactory;
    private final ImpressaoService impressaoService;
    private final UsuarioLogado usuarioLogado;
    private final LicencaService licencaService;

    /** Nome da empresa (o totem e o painel mostram no topo). */
    public EmpresaPublicaResponse empresaPublica(String slug) {
        return new EmpresaPublicaResponse(buscarEmpresa(slug).getNomeFantasia());
    }

    public List<Servico> servicos(String slug) {
        return servicoRepository.findByEmpresaIdOrderByNomeAsc(empresaLiberada(slug).getId());
    }

    public List<Prioridade> prioridades(String slug) {
        return prioridadeRepository.findByEmpresaIdOrderByPesoDescNomeAsc(empresaLiberada(slug).getId());
    }

    /**
     * Gera uma senha nova no totem.
     * <p>
     * Código = 2 primeiras letras da sigla + sequência de 3 dígitos por serviço (CA001, CA002...).
     * A linha do serviço fica TRAVADA durante a transação: dois totens ao mesmo tempo não pegam o mesmo número.
     */
    @Transactional
    public SenhaGeradaResponse gerar(String slug, Long servicoId, Long prioridadeId) {
        Empresa empresa = empresaLiberada(slug);
        Servico servico = servicoRepository.travarPorIdEEmpresa(servicoId, empresa.getId())
                .orElseThrow(() -> ErroNegocio.naoEncontrado("Serviço"));
        Prioridade prioridade = prioridadeRepository.findByIdAndEmpresaId(prioridadeId, empresa.getId())
                .orElseThrow(() -> ErroNegocio.naoEncontrado("Prioridade"));

        String sigla = servico.getSigla().toUpperCase();
        String prefixo = sigla.length() >= 2 ? sigla.substring(0, 2) : sigla;
        long jaGeradas = senhaRepository.countByServicoId(servico.getId());
        String codigo = prefixo + String.format("%03d", jaGeradas + 1);

        Senha senha = senhaRepository.save(senhaFactory.criarSenha(servico, prioridade, codigo));
        return new SenhaGeradaResponse(senha.getId(), senha.getCodigo(), servico.getNome(), senha.getDataHoraGeracao());
    }

    /** Imprime o ticket de uma senha (só se a senha for da empresa do slug). */
    @Transactional(readOnly = true)
    public void imprimir(String slug, Long senhaId) {
        Empresa empresa = empresaLiberada(slug);
        Senha senha = senhaRepository.findByIdAndEmpresaId(senhaId, empresa.getId())
                .orElseThrow(() -> ErroNegocio.naoEncontrado("Senha"));
        impressaoService.imprimirSenhaSilenciosa(senha);
    }

    /** Senhas em atendimento agora, a mais recente primeiro (o que a TV mostra). */
    @Transactional(readOnly = true)
    public List<PainelItem> painel(String slug) {
        Empresa empresa = empresaLiberada(slug);
        return atendimentoRepository.findByEmpresaIdAndDataHoraFimIsNull(empresa.getId()).stream()
                .map(a -> new PainelItem(a.getSenha().getCodigo(), a.getGuiche().getNome(), a.getDataHoraInicio()))
                .sorted(Comparator.comparing(PainelItem::inicio).reversed())
                .toList();
    }

    /**
     * Fim do dia: apaga senhas e atendimentos SÓ da empresa do usuário logado
     * e libera os guichês. A numeração volta a começar do 001.
     */
    @Transactional
    public void resetarFila() {
        Long empresaId = usuarioLogado.empresaId();
        atendimentoRepository.apagarDaEmpresa(empresaId);
        senhaRepository.apagarDaEmpresa(empresaId);
    }

    private Empresa buscarEmpresa(String slug) {
        return empresaRepository.findBySlug(slug).orElseThrow(() -> ErroNegocio.naoEncontrado("Empresa"));
    }

    /** Busca a empresa e recusa (402) se a licença estiver vencida ou bloqueada. */
    private Empresa empresaLiberada(String slug) {
        Empresa empresa = buscarEmpresa(slug);
        licencaService.exigirLiberada(empresa);
        return empresa;
    }

}
