package br.com.luizbrand.worklog.repository;

import br.com.luizbrand.worklog.entity.Systems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemRepository extends JpaRepository<Systems, Long> {

    Optional<Systems> findByPublicId(UUID publicId);
    Optional<Systems> findByName(String name);

}
