package br.com.sistemadesenhas.service;

import br.com.sistemadesenhas.dto.LicencaResponse;
import br.com.sistemadesenhas.entity.Configuracao;
import br.com.sistemadesenhas.entity.Empresa;
import br.com.sistemadesenhas.entity.enums.FormaPagamento;
import br.com.sistemadesenhas.entity.enums.SituacaoLicenca;
import br.com.sistemadesenhas.entity.enums.StatusEmpresa;
import br.com.sistemadesenhas.entity.Pagamento;
import br.com.sistemadesenhas.entity.Usuario;
import br.com.sistemadesenhas.exception.ErroNegocio;
import br.com.sistemadesenhas.repository.ConfiguracaoRepository;
import br.com.sistemadesenhas.repository.EmpresaRepository;
import br.com.sistemadesenhas.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Regras da licença (assinatura) das empresas: período grátis, vencimento, mensalidade e pagamentos.
 */
@Service
@RequiredArgsConstructor
public class LicencaService {

    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmpresaRepository empresaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ConfiguracaoRepository configuracaoRepository;

    /**
     * Calcula a situação da licença. A ordem importa: bloqueio do GM vale mais que tudo.
     */
    public SituacaoLicenca situacao(Empresa empresa) {
        if (empresa.getStatus() == StatusEmpresa.BLOQUEADA) {
            return SituacaoLicenca.BLOQUEADA;
        }
        if (LocalDate.now().isAfter(empresa.getDataVencimento())) {
            return SituacaoLicenca.VENCIDA;
        }
        return empresa.isEmPeriodoGratis() ? SituacaoLicenca.EM_TESTE : SituacaoLicenca.ATIVA;
    }

    /** @return dias até o vencimento (0 no dia do vencimento, negativo se já venceu) */
    public long diasRestantes(Empresa empresa) {
        return ChronoUnit.DAYS.between(LocalDate.now(), empresa.getDataVencimento());
    }

    /** Monta o resumo da licença para mostrar nas telas. */
    public LicencaResponse resumo(Empresa empresa) {
        SituacaoLicenca situacao = situacao(empresa);
        return new LicencaResponse(situacao, situacao.liberada(), empresa.getDataVencimento(),
                diasRestantes(empresa), empresa.isEmPeriodoGratis(), empresa.getSerial(), empresa.getCnpj(),
                valorMensal(empresa));
    }

    // ------------------------------------------------------------------ mensalidade

    /** @return quanto a empresa paga por mês: o valor próprio dela ou, se não tiver, o padrão */
    public BigDecimal valorMensal(Empresa empresa) {
        return empresa.getValorMensal() != null ? empresa.getValorMensal() : valorMensalPadrao();
    }

    /** @return mensalidade padrão, que vale para todas as empresas sem valor próprio */
    public BigDecimal valorMensalPadrao() {
        return configuracao().getValorMensalPadrao();
    }

    /** GM: muda a mensalidade padrão (ex.: reajuste de preço). Vale para as próximas cobranças. */
    @Transactional
    public BigDecimal alterarValorMensalPadrao(BigDecimal valor) {
        if (valor == null) {
            throw ErroNegocio.invalido("VALOR_OBRIGATORIO", "Informe o valor da mensalidade.");
        }
        Configuracao configuracao = configuracao();
        configuracao.setValorMensalPadrao(valor);
        return configuracaoRepository.save(configuracao).getValorMensalPadrao();
    }

    /** A linha única é criada pela migração V5; se sumir, é erro de instalação. */
    private Configuracao configuracao() {
        return configuracaoRepository.findById(Configuracao.UNICA)
                .orElseThrow(() -> new IllegalStateException("Tabela configuracoes sem a linha id = 1 (veja V5__mensalidade.sql)"));
    }

    /**
     * Interrompe a requisição com status 402 se a empresa não puder usar o sistema.
     * Chamado pelo VerificadorLicenca e pelas rotas públicas (totem e painel).
     */
    public void exigirLiberada(Empresa empresa) {
        SituacaoLicenca situacao = situacao(empresa);
        if (situacao == SituacaoLicenca.BLOQUEADA) {
            throw new ErroNegocio(HttpStatus.PAYMENT_REQUIRED, "EMPRESA_BLOQUEADA",
                    "O acesso desta empresa foi bloqueado. Entre em contato com o suporte.");
        }
        if (situacao == SituacaoLicenca.VENCIDA) {
            throw new ErroNegocio(HttpStatus.PAYMENT_REQUIRED, "LICENCA_VENCIDA",
                    "A licença desta empresa venceu em " + empresa.getDataVencimento().format(DATA_BR) + ". Regularize o pagamento.");
        }
    }

    /**
     * Registra um pagamento e empurra o vencimento.
     * <p>
     * Se a empresa ainda está dentro do prazo, os dias são somados ao vencimento atual
     * (ninguém perde dias por pagar adiantado). Se já venceu, conta a partir de hoje.
     *
     * @param registradoPor GM que registrou (ou null quando o pagamento for automático)
     */
    @Transactional
    public Pagamento registrarPagamento(Empresa empresa, BigDecimal valor, int dias, FormaPagamento forma,
                                        String observacao, Usuario registradoPor) {
        LocalDate hoje = LocalDate.now();
        LocalDate anterior = empresa.getDataVencimento();
        LocalDate base = anterior.isBefore(hoje) ? hoje : anterior;
        LocalDate novo = base.plusDays(dias);

        empresa.setDataVencimento(novo);
        empresa.setEmPeriodoGratis(false);
        empresaRepository.save(empresa);

        Pagamento pagamento = new Pagamento();
        pagamento.setEmpresa(empresa);
        pagamento.setValor(valor);
        pagamento.setDiasAdicionados(dias);
        pagamento.setForma(forma);
        pagamento.setObservacao(observacao);
        pagamento.setDataPagamento(LocalDateTime.now());
        pagamento.setVencimentoAnterior(anterior);
        pagamento.setVencimentoNovo(novo);
        pagamento.setRegistradoPor(registradoPor);
        return pagamentoRepository.save(pagamento);
    }
}
