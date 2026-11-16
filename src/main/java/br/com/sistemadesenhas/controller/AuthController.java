package br.com.sistemadesenhas.controller;

import br.com.sistemadesenhas.dto.MeResponse;
import br.com.sistemadesenhas.security.UsuarioLogado;
import br.com.sistemadesenhas.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** "Quem sou eu": todas as telas chamam ao abrir, para saber o perfil e a empresa de quem está logado. */
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UsuarioLogado usuarioLogado;

    /**
     * @param login true quando chamado pela tela de login (registra o acesso no histórico)
     */
    @GetMapping("/api/auth/me")
    public MeResponse me(@RequestParam(defaultValue = "false") boolean login,
                         HttpServletRequest request) {
        return authService.identificar(usuarioLogado.identidade(), login, ipDe(request));
    }

    /** IP real do cliente (considera proxy, quando o sistema estiver online atrás de um). */
    private String ipDe(HttpServletRequest request) {
        String encaminhado = request.getHeader("X-Forwarded-For");
        return encaminhado != null && !encaminhado.isBlank() ? encaminhado.split(",")[0].trim() : request.getRemoteAddr();
    }
}
