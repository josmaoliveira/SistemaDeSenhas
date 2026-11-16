package br.com.sistemadesenhas.security;

import br.com.sistemadesenhas.entity.Usuario;
import br.com.sistemadesenhas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Depois que o token é validado, este conversor descobre o PERFIL da pessoa no nosso banco
 * e transforma em "ROLE_GM", "ROLE_ADMIN_EMPRESA" ou "ROLE_ATENDENTE".
 * Essas roles são as usadas nas regras do {@code SecurityConfig}.
 * <p>
 * Sem role (usuário desativado, e-mail não confirmado ou ainda sem cadastro) a pessoa só
 * consegue acessar /api/auth e /api/cadastro.
 */
@Component
@RequiredArgsConstructor
public class ConversorDeToken implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioRepository usuarioRepository;
    private final LeitorDeIdentidade leitorDeIdentidade;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Identidade identidade = leitorDeIdentidade.ler(jwt);
        List<GrantedAuthority> roles = new ArrayList<>();
        if (!identidade.emailPendente()) {
            usuarioRepository.findByFirebaseUid(identidade.uid())
                    .filter(Usuario::isAtivo)
                    .ifPresent(u -> roles.add(new SimpleGrantedAuthority("ROLE_" + u.getPerfil().name())));
        }
        return new JwtAuthenticationToken(jwt, roles, identidade.uid());
    }
}
