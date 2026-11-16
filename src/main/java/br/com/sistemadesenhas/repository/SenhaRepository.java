package br.com.sistemadesenhas.repository;

import br.com.sistemadesenhas.entity.Senha;
import br.com.sistemadesenhas.entity.Servico;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SenhaRepository extends JpaRepository<Senha, Long> {

    /**
     * Fila de um serviço: senhas AGUARDANDO, primeiro as de MAIOR peso de prioridade e,
     * no empate, as mais antigas. A trava (PESSIMISTIC_WRITE) impede que dois guichês
     * chamem a mesma senha ao mesmo tempo.
     * <p>
     * Não precisa filtrar pela empresa: o serviço já pertence a uma única empresa.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Senha s " +
            "WHERE s.status = 'AGUARDANDO' AND s.servico = :servico " +
            "ORDER BY s.prioridade.peso DESC, s.dataHoraGeracao ASC")
    List<Senha> encontrarProximaSenha(Servico servico);

    Optional<Senha> findByIdAndEmpresaId(Long id, Long empresaId);

    long countByPrioridadeId(Long prioridadeId);

    long countByServicoId(Long servicoId);

    /** Apaga todas as senhas de UMA empresa (reset da fila no fim do dia). */
    @Modifying
    @Query("DELETE FROM Senha s WHERE s.empresa.id = :empresaId")
    int apagarDaEmpresa(Long empresaId);
}
