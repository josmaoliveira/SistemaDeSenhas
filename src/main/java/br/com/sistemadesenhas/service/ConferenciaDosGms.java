package br.com.sistemadesenhas.service;

import br.com.sistemadesenhas.config.AppProperties;
import br.com.sistemadesenhas.entity.enums.Perfil;
import br.com.sistemadesenhas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Roda quando o sistema sobe e avisa no terminal sobre problemas na configuração do GM.
 * <p>
 * O caso mais comum: o e-mail colocado em app.gm-emails já pertence a um usuário de empresa.
 * Um e-mail só pode ter um perfil, então essa pessoa entra na tela da empresa, e não no painel do GM.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConferenciaDosGms implements ApplicationRunner {

    private final AppProperties app;
    private final UsuarioRepository usuarioRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (app.listaEmailsGm().isEmpty()) {
            log.info("Nenhum GM configurado (app.gm-emails). O painel geral /gm.html ficará sem acesso.");
            return;
        }
        for (String email : app.listaEmailsGm()) {
            usuarioRepository.findByEmailIgnoreCase(email)
                    .filter(u -> u.getPerfil() != Perfil.GM)
                    .ifPresent(u -> log.warn("O e-mail {} está em app.gm-emails, mas já é {} de uma empresa. "
                            + "Ele vai entrar na tela da empresa, não no painel do GM. Use outro e-mail para o GM.",
                            email, u.getPerfil()));
        }
    }
}
