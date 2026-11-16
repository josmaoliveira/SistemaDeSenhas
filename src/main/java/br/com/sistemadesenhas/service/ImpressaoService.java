package br.com.sistemadesenhas.service;

import br.com.sistemadesenhas.entity.Senha;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * PADRÃO FACADE: o resto do sistema pede "imprima esta senha" sem saber que por trás
 * existe o JasperReports (veja {@link ReportPrinter} e {@link JasperReportPrinter}).
 */
@Service
@RequiredArgsConstructor
public class ImpressaoService {

    private final ReportPrinter reportPrinter;

    /** Envia o ticket para a impressora padrão do computador onde o sistema roda, sem abrir janela. */
    public void imprimirSenhaSilenciosa(Senha senha) {
        reportPrinter.printSenha(senha);
    }
}
