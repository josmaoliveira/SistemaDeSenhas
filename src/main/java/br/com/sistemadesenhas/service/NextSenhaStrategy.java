package br.com.sistemadesenhas.service;

import br.com.sistemadesenhas.entity.Senha;
import br.com.sistemadesenhas.entity.Servico;

import java.util.List;

/**
 * PADRÃO STRATEGY: define COMO escolher a próxima senha da fila.
 * Hoje a implementação é {@link PrioridadeStrategy}. Outra regra (ex.: intercalar preferencial e normal)
 * seria uma nova classe implementando esta interface, sem mexer no AtendimentoService.
 */
public interface NextSenhaStrategy {

    /** @return senhas aguardando do serviço, na ordem em que devem ser chamadas (a primeira é a próxima) */
    List<Senha> encontrarProxima(Servico servico);
}
