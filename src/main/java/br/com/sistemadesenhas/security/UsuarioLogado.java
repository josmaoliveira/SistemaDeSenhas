package br.com.sistemadesenhas.security;

import br.com.sistemadesenhas.entity.Empresa;
import br.com.sistemadesenhas.entity.Usuario;
import br.com.sistemadesenhas.exception.ErroNegocio;
import br.com.sistemadesenhas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Dá acesso ao usuário que fez a requisição atual.
 * <p>
 * É a PEÇA CENTRAL do isolamento multiempresa: os services usam {@link #empresaId()}
 * para filtrar TODAS as consultas. Como a empresa vem do token (e não da URL ou do JSON),
 * ninguém consegue "se passar" por outra empresa.
 */
@Component
@RequiredArgsConstructor
public class UsuarioLogado {

    /** Nome do atributo usado para guardar o usuário durante a requisição (evita buscar no banco várias vezes). */
    private static final String CHAVE = UsuarioLogado.class.getName();

    private final UsuarioRepository usuarioRepository;
    private final LeitorDeIdentidade leitorDeIdentidade;

    /** @return quem fez o login (e-mail, telefone...), mesmo que ainda não tenha cadastro no sistema */
    public Identidade identidade() {
        return leitorDeIdentidade.ler(token());
    }

    /** @return o token (JWT) da requisição atual */
    private Jwt token() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        throw new ErroNegocio(HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO", "Faça login para continuar.");
    }

    /** @return o usuário logado, já com a empresa carregada */
    public Usuario usuario() {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (atributos != null && atributos.getAttribute(CHAVE, RequestAttributes.SCOPE_REQUEST) instanceof Usuario u) {
            return u;
        }
        Usuario usuario = usuarioRepository.buscarPorUidComEmpresa(identidade().uid())
                .orElseThrow(() -> ErroNegocio.proibido("SEM_CADASTRO", "Usuário sem cadastro no sistema."));
        if (atributos != null) {
            atributos.setAttribute(CHAVE, usuario, RequestAttributes.SCOPE_REQUEST);
        }
        return usuario;
    }

    /** @return a empresa do usuário logado. Erro se for o GM (que não tem empresa). */
    public Empresa empresa() {
        Empresa empresa = usuario().getEmpresa();
        if (empresa == null) {
            throw ErroNegocio.proibido("SEM_EMPRESA", "Este usuário não pertence a nenhuma empresa.");
        }
        return empresa;
    }

    /** @return id da empresa do usuário logado: use em TODA consulta de dados da empresa */
    public Long empresaId() {
        return empresa().getId();
    }
}
