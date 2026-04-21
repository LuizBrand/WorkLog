package br.com.luizbrand.worklog.auth;

import br.com.luizbrand.worklog.auth.dto.AuthenticationResponse;
import br.com.luizbrand.worklog.auth.dto.LoginRequest;
import br.com.luizbrand.worklog.auth.dto.RegisterRequest;
import br.com.luizbrand.worklog.auth.dto.RegisterResponse;
import br.com.luizbrand.worklog.auth.refreshtoken.RefreshToken;
import br.com.luizbrand.worklog.auth.refreshtoken.RefreshTokenService;
import br.com.luizbrand.worklog.exception.Business.RefreshTokenException;
import br.com.luizbrand.worklog.exception.Conflict.EmailAlreadyExistsException;
import br.com.luizbrand.worklog.exception.NotFound.RoleNotFoundException;
import br.com.luizbrand.worklog.role.Role;
import br.com.luizbrand.worklog.role.RoleRepository;
import br.com.luizbrand.worklog.role.enums.RoleName;
import br.com.luizbrand.worklog.support.RefreshTokenTestBuilder;
import br.com.luizbrand.worklog.support.RoleTestBuilder;
import br.com.luizbrand.worklog.support.UserTestBuilder;
import br.com.luizbrand.worklog.user.User;
import br.com.luizbrand.worklog.user.UserMapper;
import br.com.luizbrand.worklog.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private User user;
    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = RoleTestBuilder.userRole();
        user = UserTestBuilder.aUser().withRole(userRole).build();
    }

    @Nested
    @DisplayName("Method: register()")
    class RegisterTests {

        @Test
        @DisplayName("Should register a new user with USER role and encoded password")
        void shouldRegisterUserSuccessfully() {
            RegisterRequest request = new RegisterRequest("New User", "new@worklog.test", "Password1");
            User mapped = UserTestBuilder.aUser()
                    .withName(request.name())
                    .withEmail(request.email())
                    .withPassword(request.password())
                    .withRoles(new java.util.HashSet<>())
                    .build();
            RegisterResponse expected = new RegisterResponse(
                    mapped.getPublicId().toString(), request.name(), request.email(), "2026-04-19T10:00");

            when(userService.findUserByEmail(request.email())).thenReturn(Optional.empty());
            when(roleRepository.findRoleByName(RoleName.USER)).thenReturn(Optional.of(userRole));
            when(userMapper.toUser(request)).thenReturn(mapped);
            when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
            when(userService.saveUser(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userMapper.toAuthResponse(any(User.class))).thenReturn(expected);

            RegisterResponse response = authService.register(request);

            assertThat(response).isEqualTo(expected);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userService).saveUser(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertThat(saved.getPassword()).isEqualTo("encoded-password");
            assertThat(saved.getRoles()).containsExactly(userRole);
        }

        @Test
        @DisplayName("Should throw EmailAlreadyExistsException when email is taken")
        void shouldThrowWhenEmailAlreadyExists() {
            RegisterRequest request = new RegisterRequest("New", "taken@worklog.test", "Password1");

            when(userService.findUserByEmail(request.email())).thenReturn(Optional.of(user));

            EmailAlreadyExistsException ex = assertThrows(EmailAlreadyExistsException.class,
                    () -> authService.register(request));

            assertThat(ex.getMessage()).contains(request.email());
            verify(userService, never()).saveUser(any());
            verify(roleRepository, never()).findRoleByName(any());
        }

        @Test
        @DisplayName("Should throw RoleNotFoundException when default USER role is missing")
        void shouldThrowWhenDefaultRoleMissing() {
            RegisterRequest request = new RegisterRequest("New", "new@worklog.test", "Password1");

            when(userService.findUserByEmail(request.email())).thenReturn(Optional.empty());
            when(roleRepository.findRoleByName(RoleName.USER)).thenReturn(Optional.empty());

            assertThrows(RoleNotFoundException.class, () -> authService.register(request));

            verify(userService, never()).saveUser(any());
        }
    }

    @Nested
    @DisplayName("Method: login()")
    class LoginTests {

        @Test
        @DisplayName("Should return access + refresh token on valid credentials")
        void shouldReturnTokensOnValidCredentials() {
            LoginRequest request = new LoginRequest(user.getEmail(), "Password1");
            RefreshToken refreshToken = RefreshTokenTestBuilder.aRefreshToken()
                    .withUserEmail(user.getEmail()).build();
            Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(jwtService.generateAcessToken(user)).thenReturn("access-token");
            when(refreshTokenService.generateRefreshToken(user.getEmail())).thenReturn(refreshToken);

            AuthenticationResponse response = authService.login(request);

            assertThat(response.acessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo(refreshToken.getId());
        }

        @Test
        @DisplayName("Should propagate BadCredentialsException from the AuthenticationManager")
        void shouldPropagateBadCredentials() {
            LoginRequest request = new LoginRequest(user.getEmail(), "wrong");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThrows(BadCredentialsException.class, () -> authService.login(request));

            verify(jwtService, never()).generateAcessToken(any());
            verify(refreshTokenService, never()).generateRefreshToken(anyString());
        }
    }

    @Nested
    @DisplayName("Method: refreshToken()")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should rotate tokens when refresh token is valid")
        void shouldRotateTokensWhenRefreshValid() {
            RefreshToken existing = RefreshTokenTestBuilder.aRefreshToken()
                    .withUserEmail(user.getEmail()).build();
            RefreshToken rotated = RefreshTokenTestBuilder.aRefreshToken()
                    .withUserEmail(user.getEmail()).build();

            when(refreshTokenService.findByToken(existing.getId())).thenReturn(Optional.of(existing));
            when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(user);
            when(jwtService.generateAcessToken(user)).thenReturn("new-access-token");
            when(refreshTokenService.generateRefreshToken(user.getEmail())).thenReturn(rotated);

            AuthenticationResponse response = authService.refreshToken(existing.getId());

            assertThat(response.acessToken()).isEqualTo("new-access-token");
            assertThat(response.refreshToken()).isEqualTo(rotated.getId());
            verify(refreshTokenService).deleteByToken(existing.getId());
        }

        @Test
        @DisplayName("Should throw RefreshTokenException when token not found")
        void shouldThrowWhenTokenNotFound() {
            String missing = "missing-token";
            when(refreshTokenService.findByToken(missing)).thenReturn(Optional.empty());

            assertThrows(RefreshTokenException.class, () -> authService.refreshToken(missing));

            verify(jwtService, never()).generateAcessToken(any());
            verify(refreshTokenService, never()).generateRefreshToken(anyString());
        }
    }

    @Nested
    @DisplayName("Method: logout()")
    class LogoutTests {

        @Test
        @DisplayName("Should delegate deletion to the refresh token service")
        void shouldDeleteRefreshToken() {
            String token = "token-to-invalidate";

            authService.logout(token);

            verify(refreshTokenService, times(1)).deleteByToken(token);
        }
    }
}
