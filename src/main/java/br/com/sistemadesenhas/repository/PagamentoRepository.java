package br.com.sistemadesenhas.repository;

import br.com.sistemadesenhas.entity.Pagamento;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    @Query("SELECT p FROM Pagamento p JOIN FETCH p.empresa LEFT JOIN FETCH p.registradoPor " +
            "WHERE (:empresaId IS NULL OR p.empresa.id = :empresaId) ORDER BY p.dataPagamento DESC")
    List<Pagamento> ultimos(Long empresaId, Pageable pagina);

    /** Soma dos pagamentos a partir de uma data (0 se não houver). */
    @Query("SELECT COALESCE(SUM(p.valor), 0) FROM Pagamento p WHERE p.dataPagamento >= :inicio")
    BigDecimal somaDesde(LocalDateTime inicio);
}
