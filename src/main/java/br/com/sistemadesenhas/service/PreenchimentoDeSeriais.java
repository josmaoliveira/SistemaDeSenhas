package br.com.sistemadesenhas.service;

import br.com.sistemadesenhas.entity.Empresa;
import br.com.sistemadesenhas.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Roda UMA VEZ toda vez que o sistema sobe (ApplicationRunner).
 * <p>
 * Empresas cadastradas antes da etapa 3 não têm serial (o banco não consegue calculá-lo, porque o
 * cálculo usa o segredo app.serial.segredo, que só o servidor conhece). Aqui calculamos e salvamos.
 * Depois que todas tiverem serial, não faz mais nada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PreenchimentoDeSeriais implements ApplicationRunner {

    private final EmpresaRepository empresaRepository;
    private final SerialService serialService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Empresa> semSerial = empresaRepository.findBySerialIsNull();
        for (Empresa empresa : semSerial) {
            empresa.setSerial(serialService.gerar(empresa.getCnpj(), empresa.getVersaoSerial()));
        }
        if (!semSerial.isEmpty()) {
            empresaRepository.saveAll(semSerial);
            log.info("Serial gerado para {} empresa(s) que ainda não tinham.", semSerial.size());
        }
    }
}
