package br.com.sistemadesenhas.repository;

import br.com.sistemadesenhas.entity.Prioridade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrioridadeRepository extends JpaRepository<Prioridade, Long> {

    List<Prioridade> findByEmpresaIdOrderByPesoDescNomeAsc(Long empresaId);

    Optional<Prioridade> findByIdAndEmpresaId(Long id, Long empresaId);
}
