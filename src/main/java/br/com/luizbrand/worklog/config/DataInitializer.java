package br.com.luizbrand.worklog.config;

import br.com.luizbrand.worklog.role.Role;
import br.com.luizbrand.worklog.role.RoleRepository;
import br.com.luizbrand.worklog.role.enums.RoleName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

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
                logger.info("Role {} criada.", roleName);
            }
        });

    }
}
