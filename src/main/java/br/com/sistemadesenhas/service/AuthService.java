package br.com.sistemadesenhas.service;

import br.com.sistemadesenhas.config.AppProperties;
import br.com.sistemadesenhas.dto.EmpresaResumo;
import br.com.sistemadesenhas.dto.MeResponse;
import br.com.sistemadesenhas.entity.enums.Perfil;
import br.com.sistemadesenhas.entity.RegistroLogin;
import br.com.sistemadesenhas.entity.Usuario;
import br.com.sistemadesenhas.exception.ErroNegocio;
import br.com.sistemadesenhas.repository.RegistroLoginRepository;
import br.com.sistemadesenhas.repository.UsuarioRepository;
import br.com.sistemadesenhas.security.Identidade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Liga a conta do serviço de login (Firebase) ao usuário do sistema e responde "quem sou eu".
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RegistroLoginRepository registroLoginRepository;
    private final LicencaService licencaService;
    private final AppProperties app;

    /**
     * Descobre qual usuário do sistema corresponde a quem fez o login.
     * <p>
     * Ordem de busca:
     * <ol>
     *   <li>Pelo id do Firebase (usuário que já entrou antes);</li>
     *   <li>Pelo e-mail: é o "primeiro acesso" de quem foi cadastrado pelo administrador da empresa;</li>
     *   <li>Pelo telefone, para quem entrou por SMS;</li>
     *   <li>Se o e-mail estiver em app.gm-emails, cria o usuário GM.</li>
     * </ol>
     * Se nada for encontrado, responde 404 SEM_CADASTRO e a tela leva para o cadastro de empresa.
     *
     * @param registrarLogin true quando vem da tela de login (grava no histórico de acessos)
     * @param ip             IP de quem está entrando (para o histórico)
     */
    @Transactional
    public MeResponse identificar(Identidade identidade, boolean registrarLogin, String ip) {
        if (identidade.emailPendente()) {
            throw ErroNegocio.proibido("EMAIL_NAO_VERIFICADO",
                    "Confirme seu e-mail: abra a mensagem que enviamos e clique no link.");
        }

        Usuario usuario = encontrarOuVincular(identidade)
                .orElseThrow(() -> new ErroNegocio(HttpStatus.NOT_FOUND, "SEM_CADASTRO",
                        "Você ainda não tem cadastro. Cadastre sua empresa ou peça acesso ao administrador."));

        if (!usuario.isAtivo()) {
            throw ErroNegocio.proibido("USUARIO_INATIVO", "Seu acesso foi desativado. Fale com o administrador.");
        }

        if (registrarLogin) {
            usuario.setUltimoLogin(LocalDateTime.now());
            usuarioRepository.save(usuario);

            RegistroLogin registro = new RegistroLogin();
            registro.setUsuario(usuario);
            registro.setEmpresa(usuario.getEmpresa());
            registro.setDataHora(LocalDateTime.now());
            registro.setIp(ip);
            registro.setProvedor(identidade.provedor());
            registroLoginRepository.save(registro);
        }

        return new MeResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil(),
                EmpresaResumo.de(usuario.getEmpresa()),
                usuario.getEmpresa() == null ? null : licencaService.resumo(usuario.getEmpresa()));
    }

    /** Aplica a ordem de busca descrita em {@link #identificar}. */
    private Optional<Usuario> encontrarOuVincular(Identidade identidade) {
        String uid = identidade.uid();

        Optional<Usuario> jaVinculado = usuarioRepository.buscarPorUidComEmpresa(uid);
        if (jaVinculado.isPresent()) {
            return jaVinculado;
        }

        // Primeiro acesso por e-mail: só vincula se o e-mail é confirmado (link clicado ou Google)
        String email = identidade.email();
        if (email != null && identidade.emailVerificado()) {
            Optional<Usuario> porEmail = usuarioRepository.findByEmailIgnoreCase(email)
                    .filter(u -> u.getFirebaseUid() == null);
            if (porEmail.isPresent()) {
                return Optional.of(vincular(porEmail.get(), uid));
            }
        }

        // Primeiro acesso por SMS: o telefone só vem na identidade depois de confirmado o código
        String telefone = identidade.telefone();
        if (telefone != null) {
            Optional<Usuario> porTelefone = usuarioRepository.findByTelefone(telefone)
                    .filter(u -> u.getFirebaseUid() == null);
            if (porTelefone.isPresent()) {
                return Optional.of(vincular(porTelefone.get(), uid));
            }
        }

        // GM: e-mails listados em app.gm-emails viram GM automaticamente
        if (email != null && identidade.emailVerificado() && app.listaEmailsGm().contains(email)
                && !usuarioRepository.existsByEmailIgnoreCase(email)) {
            Usuario gm = new Usuario();
            gm.setFirebaseUid(uid);
            gm.setEmail(email);
            gm.setNome(identidade.nome() != null ? identidade.nome() : "Administrador Geral");
            gm.setPerfil(Perfil.GM);
            gm.setAtivo(true);
            gm.setDataCadastro(LocalDateTime.now());
            return Optional.of(usuarioRepository.save(gm));
        }
        return Optional.empty();
    }

    private Usuario vincular(Usuario usuario, String uid) {
        usuario.setFirebaseUid(uid);
        return usuarioRepository.save(usuario);
    }
}
