package br.com.sistemadesenhas.repository;

import br.com.sistemadesenhas.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    Optional<Empresa> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByCnpj(String cnpj);

    /** Empresas que ainda não têm serial (cadastradas antes da etapa 3). */
    List<Empresa> findBySerialIsNull();

    List<Empresa> findAllByOrderByNomeFantasiaAsc();
}
