package br.com.luizbrand.worklog.repository;

import br.com.luizbrand.worklog.entity.Role;
import br.com.luizbrand.worklog.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findRoleByName(RoleName name);

}
