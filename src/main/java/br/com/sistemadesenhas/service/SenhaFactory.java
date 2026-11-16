package br.com.sistemadesenhas.service;

import br.com.sistemadesenhas.entity.enums.StatusSenha;
import br.com.sistemadesenhas.entity.Prioridade;
import br.com.sistemadesenhas.entity.Senha;
import br.com.sistemadesenhas.entity.Servico;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * PADRÃO FACTORY: centraliza a criação de uma {@link Senha}.
 * Se a estrutura da senha mudar, só esta classe muda.
 */
@Component
public class SenhaFactory {

    /**
     * Cria uma senha nova, na situação AGUARDANDO, ligada à mesma empresa do serviço.
     *
     * @param codigo código já calculado (ex.: CA001)
     */
    public Senha criarSenha(Servico servico, Prioridade prioridade, String codigo) {
        Senha nova = new Senha();
        nova.setEmpresa(servico.getEmpresa());
        nova.setServico(servico);
        nova.setPrioridade(prioridade);
        nova.setDataHoraGeracao(LocalDateTime.now());
        nova.setStatus(StatusSenha.AGUARDANDO);
        nova.setCodigo(codigo);
        return nova;
    }
}
