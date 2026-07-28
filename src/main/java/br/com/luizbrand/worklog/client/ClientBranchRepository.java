package br.com.luizbrand.worklog.client;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientBranchRepository extends JpaRepository<ClientBranch, Long> {

    Optional<ClientBranch> findByPublicId(UUID publicId);

    Optional<ClientBranch> findByDocumento(String documento);

    boolean existsByDocumento(String documento);

    /**
     * Consulta pelo publicId do cliente em vez de navegar por
     * {@code client.getBranches()}: listar filiais não deveria obrigar a carregar
     * o cliente inteiro.
     */
    List<ClientBranch> findByClientPublicId(UUID clientPublicId);

    List<ClientBranch> findByClientPublicIdAndIsEnabled(UUID clientPublicId, Boolean isEnabled);

}
