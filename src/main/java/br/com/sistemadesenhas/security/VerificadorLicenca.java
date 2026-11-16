package br.com.sistemadesenhas.security;

import br.com.sistemadesenhas.service.LicencaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Roda ANTES de cada chamada de /api/empresa e /api/atendimentos (veja WebConfig).
 * Se a licença da empresa estiver vencida ou bloqueada, a chamada é recusada com status 402.
 */
@Component
@RequiredArgsConstructor
public class VerificadorLicenca implements HandlerInterceptor {

    private final UsuarioLogado usuarioLogado;
    private final LicencaService licencaService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        licencaService.exigirLiberada(usuarioLogado.empresa());
        return true;
    }
}
