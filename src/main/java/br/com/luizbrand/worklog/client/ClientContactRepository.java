package br.com.luizbrand.worklog.client;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClientContactRepository extends JpaRepository<ClientContact, Long> {

    Optional<ClientContact> findByPublicId(UUID publicId);

}
