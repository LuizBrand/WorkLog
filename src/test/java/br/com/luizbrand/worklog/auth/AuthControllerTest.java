package br.com.luizbrand.worklog.auth;

import br.com.luizbrand.worklog.auth.dto.LoginRequest;
import br.com.luizbrand.worklog.auth.dto.RegisterRequest;
import br.com.luizbrand.worklog.auth.dto.RegisterResponse;
import br.com.luizbrand.worklog.exception.Business.RefreshTokenException;
import br.com.luizbrand.worklog.exception.Conflict.EmailAlreadyExistsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
    private AuthCookieService authCookieService;

    @MockitoBean
    private AuthFilter authFilter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUpCookieBuilders() {
        when(authCookieService.buildAccessCookie(any(String.class)))
                .thenAnswer(inv -> org.springframework.http.ResponseCookie
                        .from("worklog_access", inv.getArgument(0))
                        .httpOnly(true).secure(false).sameSite("Strict")
                        .path("/").maxAge(900).build());
        when(authCookieService.buildRefreshCookie(any(String.class)))
                .thenAnswer(inv -> org.springframework.http.ResponseCookie
                        .from("worklog_refresh", inv.getArgument(0))
                        .httpOnly(true).secure(false).sameSite("Strict")
                        .path("/worklog/auth").maxAge(3600).build());
        when(authCookieService.clearAccessCookie())
                .thenReturn(org.springframework.http.ResponseCookie
                        .from("worklog_access", "")
                        .httpOnly(true).secure(false).sameSite("Strict")
                        .path("/").maxAge(0).build());
        when(authCookieService.clearRefreshCookie())
                .thenReturn(org.springframework.http.ResponseCookie
                        .from("worklog_refresh", "")
                        .httpOnly(true).secure(false).sameSite("Strict")
                        .path("/worklog/auth").maxAge(0).build());
    }

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
        @DisplayName("Should return 204 No Content with HttpOnly access + refresh cookies on valid credentials")
        void shouldReturn204AndSetCookiesOnSuccess() throws Exception {
            LoginRequest request = new LoginRequest("user@worklog.test", "Password1");
            AuthTokens tokens = new AuthTokens("access-token", "refresh-token-id");
            when(authService.login(any(LoginRequest.class))).thenReturn(tokens);

            mockMvc.perform(post("/worklog/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent())
                    .andExpect(cookie().exists("worklog_access"))
                    .andExpect(cookie().value("worklog_access", "access-token"))
                    .andExpect(cookie().httpOnly("worklog_access", true))
                    .andExpect(cookie().path("worklog_access", "/"))
                    .andExpect(cookie().exists("worklog_refresh"))
                    .andExpect(cookie().value("worklog_refresh", "refresh-token-id"))
                    .andExpect(cookie().httpOnly("worklog_refresh", true))
                    .andExpect(cookie().path("worklog_refresh", "/worklog/auth"))
                    .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                            Matchers.hasItem(Matchers.containsString("SameSite=Strict"))));
        }

    }

    @Nested
    @DisplayName("Endpoint: POST /worklog/auth/refresh")
    class RefreshEndpoint {

        @Test
        @DisplayName("Should return 204 No Content with rotated cookies when refresh cookie is valid")
        void shouldReturn204AndRotateCookiesOnSuccess() throws Exception {
            AuthTokens tokens = new AuthTokens("new-access", "new-refresh");
            when(authService.refreshToken("old-refresh")).thenReturn(tokens);

            mockMvc.perform(post("/worklog/auth/refresh")
                            .cookie(new Cookie("worklog_refresh", "old-refresh")))
                    .andExpect(status().isNoContent())
                    .andExpect(cookie().value("worklog_access", "new-access"))
                    .andExpect(cookie().value("worklog_refresh", "new-refresh"));
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when refresh cookie is missing")
        void shouldReturn401WhenCookieMissing() throws Exception {
            mockMvc.perform(post("/worklog/auth/refresh"))
                    .andExpect(status().isUnauthorized());

            verify(authService, never()).refreshToken(any());
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when refresh cookie value is invalid")
        void shouldReturn401OnInvalidToken() throws Exception {
            String message = "Invalid or expired session. Please log in again.";
            when(authService.refreshToken("bad")).thenThrow(new RefreshTokenException(message));

            mockMvc.perform(post("/worklog/auth/refresh")
                            .cookie(new Cookie("worklog_refresh", "bad")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(message));
        }
    }

    @Nested
    @DisplayName("Endpoint: POST /worklog/auth/logout")
    class LogoutEndpoint {

        @Test
        @DisplayName("Should return 204 No Content, clear cookies, and delegate deletion to the service")
        void shouldClearCookiesAndDelegate() throws Exception {
            mockMvc.perform(post("/worklog/auth/logout")
                            .cookie(new Cookie("worklog_refresh", "refresh-to-invalidate")))
                    .andExpect(status().isNoContent())
                    .andExpect(cookie().exists("worklog_access"))
                    .andExpect(cookie().maxAge("worklog_access", 0))
                    .andExpect(cookie().exists("worklog_refresh"))
                    .andExpect(cookie().maxAge("worklog_refresh", 0));

            verify(authService, times(1)).logout("refresh-to-invalidate");
        }

        @Test
        @DisplayName("Should still clear cookies and return 204 when no refresh cookie is present")
        void shouldClearCookiesEvenWithoutSession() throws Exception {
            mockMvc.perform(post("/worklog/auth/logout"))
                    .andExpect(status().isNoContent())
                    .andExpect(cookie().maxAge("worklog_access", 0))
                    .andExpect(cookie().maxAge("worklog_refresh", 0));

            verify(authService, never()).logout(any());
        }
    }
}
