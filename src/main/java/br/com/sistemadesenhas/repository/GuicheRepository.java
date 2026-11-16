package br.com.sistemadesenhas.repository;

import br.com.sistemadesenhas.entity.Guiche;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GuicheRepository extends JpaRepository<Guiche, Long> {

    List<Guiche> findByEmpresaIdOrderByNumeroAscNomeAsc(Long empresaId);

    Optional<Guiche> findByIdAndEmpresaId(Long id, Long empresaId);

    long countByServicoId(Long servicoId);
}
