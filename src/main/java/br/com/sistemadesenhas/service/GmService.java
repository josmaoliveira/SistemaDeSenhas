package br.com.sistemadesenhas.service;

import br.com.sistemadesenhas.dto.MembroEquipeRequest;
import br.com.sistemadesenhas.dto.ResumoGmResponse;
import br.com.sistemadesenhas.entity.Empresa;
import br.com.sistemadesenhas.entity.enums.Perfil;
import br.com.sistemadesenhas.entity.enums.SituacaoLicenca;
import br.com.sistemadesenhas.entity.Usuario;
import br.com.sistemadesenhas.exception.ErroNegocio;
import br.com.sistemadesenhas.repository.EmpresaRepository;
import br.com.sistemadesenhas.repository.PagamentoRepository;
import br.com.sistemadesenhas.repository.RegistroLoginRepository;
import br.com.sistemadesenhas.repository.UsuarioRepository;
import br.com.sistemadesenhas.security.UsuarioLogado;
import br.com.sistemadesenhas.service.conta.GerenciadorContas;
import br.com.sistemadesenhas.util.Telefones;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Funções do GM (administrador geral): visão de todas as empresas e controle de todos os usuários.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GmService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final RegistroLoginRepository registroLoginRepository;
    private final PagamentoRepository pagamentoRepository;
    private final LicencaService licencaService;
    private final GerenciadorContas gerenciadorContas;
    private final UsuarioLogado usuarioLogado;

    /** Números do painel inicial: empresas por situação, usuários, logins de hoje e valor recebido no mês. */
    public ResumoGmResponse resumo() {
        List<Empresa> empresas = empresaRepository.findAll();
        Map<SituacaoLicenca, Long> porSituacao = empresas.stream()
                .collect(Collectors.groupingBy(licencaService::situacao, Collectors.counting()));
        long vencendo = empresas.stream()
                .filter(e -> licencaService.situacao(e).liberada())
                .filter(e -> licencaService.diasRestantes(e) <= 7)
                .count();
        LocalDate hoje = LocalDate.now();
        return new ResumoGmResponse(
                empresas.size(),
                porSituacao.getOrDefault(SituacaoLicenca.EM_TESTE, 0L),
                porSituacao.getOrDefault(SituacaoLicenca.ATIVA, 0L),
                porSituacao.getOrDefault(SituacaoLicenca.VENCIDA, 0L),
                porSituacao.getOrDefault(SituacaoLicenca.BLOQUEADA, 0L),
                vencendo,
                usuarioRepository.count(),
                registroLoginRepository.countByDataHoraAfter(hoje.atStartOfDay()),
                pagamentoRepository.somaDesde(hoje.withDayOfMonth(1).atStartOfDay()));
    }

    /** @param empresaId null = usuários de todas as empresas */
    public List<Usuario> listarUsuarios(Long empresaId) {
        return empresaId == null
                ? usuarioRepository.listarTodosComEmpresa()
                : usuarioRepository.listarPorEmpresaComEmpresa(empresaId);
    }

    /**
     * Corrige dados de qualquer usuário. Usuários de empresa só podem ser ADMIN_EMPRESA ou ATENDENTE;
     * o GM continua GM. Ao desativar, também desativa no Firebase quando possível.
     */
    @Transactional
    public Usuario atualizarUsuario(Long id, MembroEquipeRequest dados) {
        Usuario usuario = buscarUsuario(id);
        boolean ehGm = usuario.getPerfil() == Perfil.GM;
        if (ehGm != (dados.perfil() == Perfil.GM)) {
            throw ErroNegocio.invalido("PERFIL_INVALIDO",
                    ehGm ? "O GM não pode mudar de perfil." : "Usuários de empresa não podem virar GM.");
        }
        if (usuario.getId().equals(usuarioLogado.usuario().getId()) && Boolean.FALSE.equals(dados.ativo())) {
            throw ErroNegocio.invalido("PROPRIO_USUARIO", "Você não pode desativar a si mesmo.");
        }
        String email = dados.email().trim().toLowerCase();
        usuarioRepository.findByEmailIgnoreCase(email)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw ErroNegocio.conflito("EMAIL_EM_USO", "Este e-mail já está em uso por outro usuário.");
                });
        if (usuario.isPrimeiroAcessoFeito() && usuario.getEmail() != null && !usuario.getEmail().equalsIgnoreCase(email)) {
            throw ErroNegocio.invalido("EMAIL_FIXO", "O e-mail não pode ser alterado depois do primeiro acesso.");
        }
        String telefone = Telefones.normalizar(dados.telefone());
        if (telefone != null) {
            usuarioRepository.findByTelefone(telefone)
                    .filter(outro -> !outro.getId().equals(id))
                    .ifPresent(outro -> {
                        throw ErroNegocio.conflito("TELEFONE_EM_USO", "Este telefone já está em uso por outro usuário.");
                    });
        }

        boolean ativo = dados.ativo() == null || dados.ativo();
        boolean mudouAtivo = ativo != usuario.isAtivo();
        usuario.setNome(dados.nome().trim());
        usuario.setEmail(email);
        usuario.setTelefone(telefone);
        usuario.setPerfil(dados.perfil());
        usuario.setAtivo(ativo);
        usuarioRepository.save(usuario);

        // O bloqueio principal é no NOSSO banco (ativo=false já impede o acesso).
        // No Firebase é um reforço, só quando a estratégia permite.
        if (mudouAtivo && usuario.isPrimeiroAcessoFeito() && gerenciadorContas.suportaAlteracaoDireta()) {
            gerenciadorContas.definirAtiva(usuario.getFirebaseUid(), ativo);
        }
        return usuario;
    }

    /** Exclui o usuário do sistema e, se possível, a conta dele no Firebase. */
    @Transactional
    public void excluirUsuario(Long id) {
        Usuario usuario = buscarUsuario(id);
        if (usuario.getId().equals(usuarioLogado.usuario().getId())) {
            throw ErroNegocio.invalido("PROPRIO_USUARIO", "Você não pode excluir a si mesmo.");
        }
        usuarioRepository.delete(usuario);
        if (usuario.isPrimeiroAcessoFeito() && gerenciadorContas.suportaAlteracaoDireta()) {
            try {
                gerenciadorContas.excluirConta(usuario.getFirebaseUid());
            } catch (ErroNegocio e) {
                // A conta do Firebase sem cadastro aqui não acessa nada; só registra no log
                log.warn("Usuário {} excluído do sistema, mas não do Firebase: {}", id, e.getMessage());
            }
        }
    }

    /** Envia ao usuário o e-mail do Firebase para criar uma nova senha. */
    public void enviarRedefinicaoSenha(Long id) {
        Usuario usuario = buscarUsuario(id);
        if (usuario.getEmail() == null) {
            throw ErroNegocio.invalido("SEM_EMAIL", "Este usuário não tem e-mail cadastrado.");
        }
        gerenciadorContas.enviarEmailRedefinicaoSenha(usuario.getEmail());
    }

    /** Define a senha diretamente (só com a estratégia completa do Firebase). */
    public void definirSenha(Long id, String novaSenha) {
        Usuario usuario = buscarUsuario(id);
        if (!usuario.isPrimeiroAcessoFeito()) {
            throw ErroNegocio.invalido("SEM_PRIMEIRO_ACESSO", "Este usuário ainda não fez o primeiro acesso.");
        }
        gerenciadorContas.definirSenha(usuario.getFirebaseUid(), novaSenha);
    }

    /** @return informações para o painel saber quais botões mostrar */
    public Map<String, Object> capacidades() {
        return Map.of("gerenciadorContas", gerenciadorContas.nome(),
                "alteracaoDireta", gerenciadorContas.suportaAlteracaoDireta());
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> ErroNegocio.naoEncontrado("Usuário"));
    }
}
