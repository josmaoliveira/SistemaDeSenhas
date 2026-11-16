package br.com.sistemadesenhas.service.conta;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import br.com.sistemadesenhas.config.FirebaseProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Escolhe QUAL estratégia de {@link GerenciadorContas} usar, uma única vez, quando o sistema sobe:
 * com firebase.credenciais configurado usa a completa (Admin); senão, a básica (REST).
 */
@Slf4j
@Configuration
public class ContasConfig {

    @Bean
    public GerenciadorContas gerenciadorContas(FirebaseProperties firebase) {
        GerenciadorContas basica = new FirebaseRestGerenciadorContas(firebase);
        if (!firebase.temCredenciais()) {
            log.info("Gerenciador de contas: básico (sem firebase.credenciais).");
            return basica;
        }
        try (InputStream json = new FileInputStream(firebase.credenciais())) {
            FirebaseOptions opcoes = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(json))
                    .setProjectId(firebase.projectId())
                    .build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty() ? FirebaseApp.initializeApp(opcoes) : FirebaseApp.getInstance();
            log.info("Gerenciador de contas: completo (Firebase Admin).");
            return new FirebaseAdminGerenciadorContas(app, basica);
        } catch (IOException e) {
            log.error("Não foi possível ler {}. Usando o gerenciador básico.", firebase.credenciais(), e);
            return basica;
        }
    }
}
