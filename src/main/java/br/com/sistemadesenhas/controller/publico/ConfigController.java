package br.com.sistemadesenhas.controller.publico;

import br.com.sistemadesenhas.config.FirebaseProperties;
import br.com.sistemadesenhas.dto.ConfigPublicaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entrega ao navegador a configuração do Firebase. Assim os valores ficam só no application.properties
 * e não precisam ser copiados em cada arquivo HTML.
 */
@RestController
@RequiredArgsConstructor
public class ConfigController {

    private final FirebaseProperties firebase;

    @GetMapping("/api/publico/config")
    public ConfigPublicaResponse config() {
        return new ConfigPublicaResponse(firebase.configurado(), firebase.apiKey(), firebase.authDomainEfetivo(),
                firebase.projectId(), firebase.appId(), firebase.smsHabilitado());
    }
}
