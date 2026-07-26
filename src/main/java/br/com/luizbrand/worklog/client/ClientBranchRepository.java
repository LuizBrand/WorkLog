package br.com.luizbrand.worklog.client;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClientBranchRepository extends JpaRepository<ClientBranch, Long> {

    Optional<ClientBranch> findByPublicId(UUID publicId);

    Optional<ClientBranch> findByDocumento(String documento);

    boolean existsByDocumento(String documento);

}
