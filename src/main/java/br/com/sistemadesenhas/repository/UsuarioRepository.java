package br.com.sistemadesenhas.repository;

import br.com.sistemadesenhas.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByFirebaseUid(String firebaseUid);

    /** Mesmo que findByFirebaseUid, mas já traz a empresa junto (uma consulta só). */
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.empresa WHERE u.firebaseUid = :uid")
    Optional<Usuario> buscarPorUidComEmpresa(String uid);

    /** Busca por e-mail ignorando maiúsculas/minúsculas. */
    Optional<Usuario> findByEmailIgnoreCase(String email);

    Optional<Usuario> findByTelefone(String telefone);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByTelefone(String telefone);

    // ---- Consultas SEMPRE filtradas pela empresa (isolamento multiempresa) ----

    List<Usuario> findByEmpresaIdOrderByNomeAsc(Long empresaId);

    Optional<Usuario> findByIdAndEmpresaId(Long id, Long empresaId);

    // ---- Consultas do GM (enxerga todas as empresas) ----

    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.empresa ORDER BY u.nome")
    List<Usuario> listarTodosComEmpresa();

    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.empresa WHERE u.empresa.id = :empresaId ORDER BY u.nome")
    List<Usuario> listarPorEmpresaComEmpresa(Long empresaId);
}
