package br.com.sistemadesenhas.service;

import br.com.sistemadesenhas.dto.MembroEquipeRequest;
import br.com.sistemadesenhas.entity.Empresa;
import br.com.sistemadesenhas.entity.enums.Perfil;
import br.com.sistemadesenhas.entity.Usuario;
import br.com.sistemadesenhas.exception.ErroNegocio;
import br.com.sistemadesenhas.repository.UsuarioRepository;
import br.com.sistemadesenhas.security.UsuarioLogado;
import br.com.sistemadesenhas.util.Telefones;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Equipe da empresa (atendentes e outros administradores), gerenciada pelo administrador da empresa.
 * <p>
 * Como o login é do Firebase, aqui NÃO definimos senha: o administrador cadastra nome + e-mail,
 * e a pessoa faz o "primeiro acesso" na tela de login (cria a senha ou entra com o Google).
 */
@Service
@RequiredArgsConstructor
public class EquipeService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioLogado usuarioLogado;

    /** @return membros da empresa do usuário logado */
    public List<Usuario> listar() {
        return usuarioRepository.findByEmpresaIdOrderByNomeAsc(usuarioLogado.empresaId());
    }

    @Transactional
    public Usuario adicionar(MembroEquipeRequest dados) {
        Empresa empresa = usuarioLogado.empresa();
        Usuario novo = new Usuario();
        novo.setEmpresa(empresa);
        novo.setDataCadastro(LocalDateTime.now());
        preencher(novo, dados);
        return usuarioRepository.save(novo);
    }

    @Transactional
    public Usuario atualizar(Long id, MembroEquipeRequest dados) {
        Usuario membro = buscar(id);
        boolean ehVoceMesmo = membro.getId().equals(usuarioLogado.usuario().getId());
        if (ehVoceMesmo && (dados.perfil() != Perfil.ADMIN_EMPRESA || Boolean.FALSE.equals(dados.ativo()))) {
            throw ErroNegocio.invalido("PROPRIO_USUARIO", "Você não pode tirar o seu próprio acesso de administrador.");
        }
        // Depois do primeiro acesso o e-mail fica preso à conta do Firebase
        if (membro.isPrimeiroAcessoFeito() && !membro.getEmail().equalsIgnoreCase(dados.email().trim())) {
            throw ErroNegocio.invalido("EMAIL_FIXO", "O e-mail não pode ser alterado depois do primeiro acesso.");
        }
        preencher(membro, dados);
        return usuarioRepository.save(membro);
    }

    @Transactional
    public void remover(Long id) {
        Usuario membro = buscar(id);
        if (membro.getId().equals(usuarioLogado.usuario().getId())) {
            throw ErroNegocio.invalido("PROPRIO_USUARIO", "Você não pode remover a si mesmo.");
        }
        usuarioRepository.delete(membro);
    }

    /** Busca o membro SOMENTE dentro da empresa do usuário logado. */
    private Usuario buscar(Long id) {
        return usuarioRepository.findByIdAndEmpresaId(id, usuarioLogado.empresaId())
                .orElseThrow(() -> ErroNegocio.naoEncontrado("Usuário"));
    }

    /** Copia os dados do formulário, validando perfil, e-mail e telefone repetidos. */
    private void preencher(Usuario usuario, MembroEquipeRequest dados) {
        if (dados.perfil() == Perfil.GM) {
            throw ErroNegocio.proibido("PERFIL_INVALIDO", "Perfil não permitido.");
        }
        String email = dados.email().trim().toLowerCase();
        usuarioRepository.findByEmailIgnoreCase(email)
                .filter(outro -> !outro.getId().equals(usuario.getId()))
                .ifPresent(outro -> {
                    throw ErroNegocio.conflito("EMAIL_EM_USO", "Este e-mail já está em uso por outro usuário.");
                });
        String telefone = Telefones.normalizar(dados.telefone());
        if (telefone != null) {
            usuarioRepository.findByTelefone(telefone)
                    .filter(outro -> !outro.getId().equals(usuario.getId()))
                    .ifPresent(outro -> {
                        throw ErroNegocio.conflito("TELEFONE_EM_USO", "Este telefone já está em uso por outro usuário.");
                    });
        }
        usuario.setNome(dados.nome().trim());
        usuario.setEmail(email);
        usuario.setTelefone(telefone);
        usuario.setPerfil(dados.perfil());
        usuario.setAtivo(dados.ativo() == null || dados.ativo());
    }
}
