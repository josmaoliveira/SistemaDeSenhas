package br.com.sistemadesenhas.config;

import br.com.sistemadesenhas.security.ConversorDeToken;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Regras de segurança.
 * <p>
 * COMO O LOGIN FUNCIONA:
 * <ol>
 *   <li>O navegador faz login direto no Firebase (e-mail/senha, Google ou SMS).</li>
 *   <li>O Firebase devolve um "ID token" (um JWT assinado pelo Google).</li>
 *   <li>Em toda chamada para /api, o navegador manda o cabeçalho {@code Authorization: Bearer <token>}.</li>
 *   <li>Aqui o Spring confere o token com o {@code JwtDecoder} de {@link FirebaseConfig}
 *       (assinatura, validade e se foi emitido para o NOSSO projeto). Token falso ou vencido = 401.</li>
 *   <li>O {@link ConversorDeToken} descobre o perfil (GM, ADMIN_EMPRESA, ATENDENTE) no nosso banco.</li>
 * </ol>
 * Esta classe não sabe qual serviço de login é usado: o que é do Firebase fica em {@link FirebaseConfig}.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filtroDeSeguranca(HttpSecurity http, ConversorDeToken conversor) throws Exception {
        http
                // API sem sessão: cada chamada traz o token, então não há cookie para proteger contra CSRF
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(regras -> regras
                        // Totem, painel e configuração do Firebase: sem login
                        .requestMatchers("/api/publico/**").permitAll()
                        // Painel geral: só GM
                        .requestMatchers("/api/gm/**").hasRole("GM")
                        // Atendente precisa listar os guichês para escolher o seu
                        .requestMatchers(HttpMethod.GET, "/api/empresa/guiches").hasAnyRole("ADMIN_EMPRESA", "ATENDENTE")
                        // Restante da gestão da empresa: só o administrador da empresa
                        .requestMatchers("/api/empresa/**").hasRole("ADMIN_EMPRESA")
                        .requestMatchers("/api/atendimentos/**").hasAnyRole("ADMIN_EMPRESA", "ATENDENTE")
                        // Descobrir "quem sou eu" e cadastrar empresa: basta estar logado no Firebase
                        .requestMatchers("/api/auth/**", "/api/cadastro/**").authenticated()
                        // Qualquer outra rota de API não listada acima fica bloqueada
                        .requestMatchers("/api/**").denyAll()
                        // Páginas HTML, JavaScript, áudio: liberados (a proteção está na API)
                        .anyRequest().permitAll())
                .oauth2ResourceServer(o -> o.jwt(jwt -> jwt.jwtAuthenticationConverter(conversor)));
        return http.build();
    }
}
