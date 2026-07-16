package br.com.luizbrand.worklog.config;

import br.com.luizbrand.worklog.role.Role;
import br.com.luizbrand.worklog.role.RoleRepository;
import br.com.luizbrand.worklog.role.enums.RoleName;
import br.com.luizbrand.worklog.user.User;
import br.com.luizbrand.worklog.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Cria o primeiro usuário ADMIN a partir de {@link AdminSeedProperties} caso ele
 * ainda não exista. Idempotente e opcional: sem email/senha configurados, não faz
 * nada. Roda depois do {@link DataInitializer} (as roles precisam existir).
 */
@Component
@Order(2)
public class AdminSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminSeeder.class);

    private final AdminSeedProperties properties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(AdminSeedProperties properties,
                       UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        final String email = properties.email();
        final String password = properties.password();

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            logger.debug("Seed do admin desativado (worklog.admin.email/password ausentes).");
            return;
        }

        if (userRepository.findByEmail(email).isPresent()) {
            logger.info("Seed do admin ignorado: já existe usuário com email {}.", email);
            return;
        }

        Role adminRole = roleRepository.findRoleByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "Role ADMIN não encontrada — o seed do admin depende do DataInitializer."));

        User admin = User.builder()
                .name(properties.name())
                .email(email)
                .password(passwordEncoder.encode(password))
                .roles(Set.of(adminRole))
                .build();

        userRepository.save(admin);
        logger.info("Usuário ADMIN inicial criado: {}.", email);
    }
}
