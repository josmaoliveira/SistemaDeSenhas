package br.com.sistemadesenhas.repository;

import br.com.sistemadesenhas.entity.Atendimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {

    /** Atendimento em andamento (sem data de fim) de um guichê. */
    Optional<Atendimento> findByGuicheIdAndDataHoraFimIsNull(Long guicheId);

    Optional<Atendimento> findByIdAndEmpresaId(Long id, Long empresaId);

    long countByGuicheServicoId(Long servicoId);

    /** Atendimentos em andamento de uma empresa (é o que aparece no painel da TV). */
    List<Atendimento> findByEmpresaIdAndDataHoraFimIsNull(Long empresaId);

    List<Atendimento> findTop10ByGuicheIdOrderByDataHoraInicioDesc(Long guicheId);

    /** Atendimentos da empresa iniciados a partir de um momento (ex.: o começo do dia), já com o guichê. */
    @Query("SELECT a FROM Atendimento a JOIN FETCH a.guiche WHERE a.empresa.id = :empresaId AND a.dataHoraInicio >= :desde")
    List<Atendimento> iniciadosDesde(Long empresaId, LocalDateTime desde);

    @Modifying
    @Query("DELETE FROM Atendimento a WHERE a.empresa.id = :empresaId")
    int apagarDaEmpresa(Long empresaId);
}
