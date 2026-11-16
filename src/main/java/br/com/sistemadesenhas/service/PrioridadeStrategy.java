package br.com.sistemadesenhas.service;

import br.com.sistemadesenhas.entity.Senha;
import br.com.sistemadesenhas.entity.Servico;
import br.com.sistemadesenhas.repository.SenhaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Estratégia padrão: maior peso de prioridade primeiro; no empate, quem chegou antes. */
@Service
@RequiredArgsConstructor
public class PrioridadeStrategy implements NextSenhaStrategy {

    private final SenhaRepository senhaRepository;

    @Override
    public List<Senha> encontrarProxima(Servico servico) {
        return senhaRepository.encontrarProximaSenha(servico);
    }
}
