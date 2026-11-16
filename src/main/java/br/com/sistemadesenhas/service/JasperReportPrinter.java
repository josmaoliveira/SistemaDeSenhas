package br.com.sistemadesenhas.service;

import br.com.sistemadesenhas.entity.Senha;
import br.com.sistemadesenhas.exception.ErroNegocio;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Implementação de {@link ReportPrinter} usando o JasperReports (arquivo reports/Senha.jasper). */
@Slf4j
@Service
public class JasperReportPrinter implements ReportPrinter {

    /** O nome precisa bater com o arquivo, inclusive maiúsculas (no Linux faz diferença). */
    private static final String RELATORIO = "/reports/Senha.jasper";

    @Override
    public void printSenha(Senha senha) {
        try (InputStream reportStream = getClass().getResourceAsStream(RELATORIO)) {
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("CODIGO", senha.getCodigo());
            parametros.put("SERVICO", senha.getServico().getNome());

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(Collections.singletonList(senha));

            JasperPrint jasperPrint = JasperFillManager.fillReport(reportStream, parametros, dataSource);
            JasperPrintManager.printReport(jasperPrint, false); // false = sem abrir a janela de impressão
        } catch (Exception e) {
            log.warn("Falha ao imprimir a senha {}: {}", senha.getCodigo(), e.getMessage());
            throw new ErroNegocio(HttpStatus.SERVICE_UNAVAILABLE, "IMPRESSORA_INDISPONIVEL",
                    "Não foi possível imprimir. Confira se há uma impressora padrão instalada.");
        }
    }
}
