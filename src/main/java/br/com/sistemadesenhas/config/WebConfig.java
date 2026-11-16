package br.com.sistemadesenhas.config;

import br.com.sistemadesenhas.security.VerificadorLicenca;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registra o {@link VerificadorLicenca} nas rotas usadas pela empresa no dia a dia. */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final VerificadorLicenca verificadorLicenca;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(verificadorLicenca)
                .addPathPatterns("/api/empresa/**", "/api/atendimentos/**")
                // A tela de licença continua acessível com a licença vencida (para a empresa ver e pagar)
                .excludePathPatterns("/api/empresa/licenca/**");
    }
}
