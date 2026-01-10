package br.com.luizbrand.worklog.system;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemRepository extends JpaRepository<Systems, Long> {

    Optional<Systems> findByPublicId(UUID publicId);
    Optional<Systems> findByName(String name);

    List<Systems> findAllByPublicIdIn(List<UUID> publicIds);
}
