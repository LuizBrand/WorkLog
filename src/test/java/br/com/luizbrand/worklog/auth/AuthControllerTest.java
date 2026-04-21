package br.com.luizbrand.worklog.auth;

import br.com.luizbrand.worklog.auth.dto.AuthenticationResponse;
import br.com.luizbrand.worklog.auth.dto.LoginRequest;
import br.com.luizbrand.worklog.auth.dto.RegisterRequest;
import br.com.luizbrand.worklog.auth.dto.RegisterResponse;
import br.com.luizbrand.worklog.auth.refreshtoken.RefreshTokenRequest;
import br.com.luizbrand.worklog.exception.Business.RefreshTokenException;
import br.com.luizbrand.worklog.exception.Conflict.EmailAlreadyExistsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AuthFilter authFilter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Nested
    @DisplayName("Endpoint: POST /worklog/auth/register")
    class RegisterEndpoint {

        private RegisterRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new RegisterRequest("New User", "new@worklog.test", "Password1");
        }

        @Test
        @DisplayName("Should return 201 Created with the registered user payload")
        void shouldReturn201OnSuccess() throws Exception {
            RegisterResponse response = new RegisterResponse(
                    "00000000-0000-0000-0000-000000000001",
                    validRequest.name(),
                    validRequest.email(),
                    "2026-04-19T10:00");
            when(authService.register(any(RegisterRequest.class))).thenReturn(response);

            mockMvc.perform(post("/worklog/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.publicId").value(response.publicId()))
                    .andExpect(jsonPath("$.email").value(response.email()))
                    .andExpect(jsonPath("$.name").value(response.name()));
        }

        @Test
        @DisplayName("Should return 409 Conflict when email already exists")
        void shouldReturn409WhenEmailExists() throws Exception {
            String message = "Email '" + validRequest.email() + "' já está em uso.";
            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new EmailAlreadyExistsException(message));

            mockMvc.perform(post("/worklog/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(message));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when payload fails validation")
        void shouldReturn400OnValidationFailure() throws Exception {
            RegisterRequest invalid = new RegisterRequest("", "not-an-email", "short");

            mockMvc.perform(post("/worklog/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }

    @Nested
    @DisplayName("Endpoint: POST /worklog/auth/login")
    class LoginEndpoint {

        @Test
        @DisplayName("Should return 200 OK with tokens on valid credentials")
        void shouldReturn200OnSuccess() throws Exception {
            LoginRequest request = new LoginRequest("user@worklog.test", "Password1");
            AuthenticationResponse response = new AuthenticationResponse("access-token", "refresh-token-id");
            when(authService.login(any(LoginRequest.class))).thenReturn(response);

            mockMvc.perform(post("/worklog/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.acessToken").value(response.acessToken()))
                    .andExpect(jsonPath("$.refreshToken").value(response.refreshToken()));
        }

    }

    @Nested
    @DisplayName("Endpoint: POST /worklog/auth/refresh")
    class RefreshEndpoint {

        @Test
        @DisplayName("Should return 200 OK with rotated tokens on valid refresh token")
        void shouldReturn200OnSuccess() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest("old-refresh");
            AuthenticationResponse response = new AuthenticationResponse("new-access", "new-refresh");
            when(authService.refreshToken("old-refresh")).thenReturn(response);

            mockMvc.perform(post("/worklog/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.acessToken").value(response.acessToken()))
                    .andExpect(jsonPath("$.refreshToken").value(response.refreshToken()));
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when refresh token is invalid")
        void shouldReturn401OnInvalidToken() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest("bad");
            String message = "Invalid or expired session. Please log in again.";
            when(authService.refreshToken("bad")).thenThrow(new RefreshTokenException(message));

            mockMvc.perform(post("/worklog/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(message));
        }
    }

    @Nested
    @DisplayName("Endpoint: POST /worklog/auth/logout")
    class LogoutEndpoint {

        @Test
        @DisplayName("Should return 204 No Content and delegate deletion to the service")
        void shouldReturn204OnSuccess() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest("refresh-to-invalidate");

            mockMvc.perform(post("/worklog/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            verify(authService, times(1)).logout(request.refreshToken());
        }
    }
}
