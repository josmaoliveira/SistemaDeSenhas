package br.com.sistemadesenhas.repository;

import br.com.sistemadesenhas.entity.RegistroLogin;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface RegistroLoginRepository extends JpaRepository<RegistroLogin, Long> {

    /** Últimos logins, com usuário e empresa já carregados (para o painel do GM). */
    @Query("SELECT r FROM RegistroLogin r JOIN FETCH r.usuario LEFT JOIN FETCH r.empresa " +
            "WHERE (:empresaId IS NULL OR r.empresa.id = :empresaId) ORDER BY r.dataHora DESC")
    List<RegistroLogin> ultimos(Long empresaId, Pageable pagina);

    long countByDataHoraAfter(LocalDateTime inicio);
}
