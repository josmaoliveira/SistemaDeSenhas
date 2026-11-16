package br.com.sistemadesenhas.controller.gm;

import br.com.sistemadesenhas.dto.*;
import br.com.sistemadesenhas.entity.Empresa;
import br.com.sistemadesenhas.repository.EmpresaRepository;
import br.com.sistemadesenhas.repository.PagamentoRepository;
import br.com.sistemadesenhas.security.UsuarioLogado;
import br.com.sistemadesenhas.service.EmpresaService;
import br.com.sistemadesenhas.service.LicencaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Painel do GM: controle de todas as empresas e dos pagamentos. Só perfil GM (veja SecurityConfig). */
@RestController
@RequestMapping("/api/gm")
@RequiredArgsConstructor
public class GmEmpresaController {

    private final EmpresaRepository empresaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final EmpresaService empresaService;
    private final LicencaService licencaService;
    private final UsuarioLogado usuarioLogado;

    @GetMapping("/empresas")
    public List<EmpresaGmResponse> listar() {
        return empresaRepository.findAllByOrderByNomeFantasiaAsc().stream().map(empresaService::paraGm).toList();
    }

    @GetMapping("/empresas/{id}")
    public EmpresaGmResponse buscar(@PathVariable Long id) {
        return empresaService.paraGm(empresaService.buscar(id));
    }

    @PutMapping("/empresas/{id}")
    public EmpresaGmResponse atualizar(@PathVariable Long id, @RequestBody @Valid EmpresaGmRequest dados) {
        return empresaService.paraGm(empresaService.atualizar(id, dados));
    }

    @PutMapping("/empresas/{id}/status")
    public EmpresaGmResponse status(@PathVariable Long id, @RequestBody @Valid StatusEmpresaRequest dados) {
        return empresaService.paraGm(empresaService.alterarStatus(id, dados.status()));
    }

    @PutMapping("/empresas/{id}/vencimento")
    public EmpresaGmResponse vencimento(@PathVariable Long id, @RequestBody @Valid VencimentoRequest dados) {
        return empresaService.paraGm(empresaService.alterarVencimento(id, dados.dataVencimento()));
    }

    /** Mensalidade própria da empresa; valorMensal vazio = volta ao valor padrão. */
    @PutMapping("/empresas/{id}/valor-mensal")
    public EmpresaGmResponse valorMensal(@PathVariable Long id, @RequestBody @Valid ValorMensalRequest dados) {
        return empresaService.paraGm(empresaService.alterarValorMensal(id, dados.valorMensal()));
    }

    @PostMapping("/empresas/{id}/novo-serial")
    public EmpresaGmResponse novoSerial(@PathVariable Long id) {
        return empresaService.paraGm(empresaService.gerarNovoSerial(id));
    }

    /** Exclui a empresa e TODOS os dados dela. */
    @DeleteMapping("/empresas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        empresaService.excluir(id);
    }

    // ---------------- mensalidade padrão (vale para todas as empresas sem valor próprio)

    @GetMapping("/mensalidade")
    public MensalidadeResponse mensalidade() {
        return new MensalidadeResponse(licencaService.valorMensalPadrao());
    }

    @PutMapping("/mensalidade")
    public MensalidadeResponse alterarMensalidade(@RequestBody @Valid ValorMensalRequest dados) {
        return new MensalidadeResponse(licencaService.alterarValorMensalPadrao(dados.valorMensal()));
    }

    // ---------------- pagamentos

    /** @param empresaId opcional: filtra por empresa */
    @GetMapping("/pagamentos")
    public List<PagamentoResponse> pagamentos(@RequestParam(required = false) Long empresaId) {
        return pagamentoRepository.ultimos(empresaId, PageRequest.of(0, 200)).stream().map(PagamentoResponse::de).toList();
    }

    /** Registra um pagamento manual e renova o vencimento. */
    @PostMapping("/empresas/{id}/pagamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public PagamentoResponse registrarPagamento(@PathVariable Long id, @RequestBody @Valid PagamentoRequest dados) {
        Empresa empresa = empresaService.buscar(id);
        return PagamentoResponse.de(licencaService.registrarPagamento(empresa, dados.valor(), dados.dias(),
                dados.forma(), dados.observacao(), usuarioLogado.usuario()));
    }
}
