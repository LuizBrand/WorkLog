package br.com.luizbrand.worklog.config;

import br.com.luizbrand.worklog.role.Role;
import br.com.luizbrand.worklog.role.RoleRepository;
import br.com.luizbrand.worklog.role.enums.RoleName;
import br.com.luizbrand.worklog.user.User;
import br.com.luizbrand.worklog.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminSeeder — bootstrap do primeiro ADMIN via env")
class AdminSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminSeeder seeder(String email, String password) {
        return new AdminSeeder(
                new AdminSeedProperties(email, password, "Admin"),
                userRepository, roleRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Cria o admin com senha codificada e role ADMIN quando configurado e ausente")
    void createsAdminWhenConfiguredAndAbsent() throws Exception {
        Role admin = Role.builder().name(RoleName.ADMIN).build();
        when(userRepository.findByEmail("admin@worklog.test")).thenReturn(Optional.empty());
        when(roleRepository.findRoleByName(RoleName.ADMIN)).thenReturn(Optional.of(admin));
        when(passwordEncoder.encode("Str0ngPass")).thenReturn("ENCODED");

        seeder("admin@worklog.test", "Str0ngPass").run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("admin@worklog.test");
        assertThat(saved.getName()).isEqualTo("Admin");
        assertThat(saved.getPassword()).isEqualTo("ENCODED");
        assertThat(saved.getRoles()).containsExactly(admin);
    }

    @Test
    @DisplayName("Não cria nada quando já existe usuário com o email")
    void skipsWhenAdminAlreadyExists() throws Exception {
        when(userRepository.findByEmail("admin@worklog.test"))
                .thenReturn(Optional.of(User.builder().build()));

        seeder("admin@worklog.test", "Str0ngPass").run();

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("É no-op quando o email não está configurado")
    void skipsWhenEmailBlank() throws Exception {
        seeder("   ", "Str0ngPass").run();

        verifyNoInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    @DisplayName("É no-op quando a senha não está configurada")
    void skipsWhenPasswordBlank() throws Exception {
        seeder("admin@worklog.test", "").run();

        verifyNoInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Falha explicitamente se a role ADMIN não existir")
    void throwsWhenAdminRoleMissing() {
        when(userRepository.findByEmail("admin@worklog.test")).thenReturn(Optional.empty());
        when(roleRepository.findRoleByName(RoleName.ADMIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> seeder("admin@worklog.test", "Str0ngPass").run())
                .isInstanceOf(IllegalStateException.class);

        verify(userRepository, never()).save(any());
    }
}
