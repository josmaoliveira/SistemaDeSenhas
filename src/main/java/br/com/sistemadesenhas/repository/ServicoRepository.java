package br.com.sistemadesenhas.repository;

import br.com.sistemadesenhas.entity.Servico;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Todas as consultas recebem o id da empresa: assim uma empresa nunca acessa o serviço de outra,
 * mesmo que alguém tente mandar o id de outra empresa na URL.
 */
public interface ServicoRepository extends JpaRepository<Servico, Long> {

    List<Servico> findByEmpresaIdOrderByNomeAsc(Long empresaId);

    Optional<Servico> findByIdAndEmpresaId(Long id, Long empresaId);

    /**
     * Mesmo que findByIdAndEmpresaId, mas TRAVA a linha até o fim da transação.
     * Usado ao gerar senha: dois totens ao mesmo tempo não geram o mesmo código.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Servico s WHERE s.id = :id AND s.empresa.id = :empresaId")
    Optional<Servico> travarPorIdEEmpresa(Long id, Long empresaId);
}
