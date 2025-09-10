package br.com.luizbrand.worklog.config;

import br.com.luizbrand.worklog.entity.Role;
import br.com.luizbrand.worklog.enums.RoleName;
import br.com.luizbrand.worklog.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        Arrays.stream(RoleName.values()).forEach(roleName -> {
            if (roleRepository.findRoleByName(roleName).isEmpty()) {
                Role defaultRole = Role.builder()
                        .name(roleName)
                        .build();
                roleRepository.save(defaultRole);
                System.out.println("Role " + roleName + " criada.");
            }
        });

    }
}
