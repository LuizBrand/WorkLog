package br.com.luizbrand.worklog.auth;

import br.com.luizbrand.worklog.auth.dto.RegisterRequest;
import br.com.luizbrand.worklog.auth.dto.RegisterResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Security: POST /worklog/auth/register is ADMIN-only")
class AuthRegisterSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    private String validBody() throws Exception {
        return objectMapper.writeValueAsString(
                new RegisterRequest("New User", "new@worklog.test", "Password1"));
    }

    @Test
    @DisplayName("Anonymous request is rejected and never reaches the service")
    void anonymousCannotRegister() throws Exception {
        mockMvc.perform(post("/worklog/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden());

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("Authenticated USER (non-admin) is rejected and never reaches the service")
    void nonAdminCannotRegister() throws Exception {
        mockMvc.perform(post("/worklog/auth/register")
                        .with(user("user@worklog.test").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden());

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("Authenticated ADMIN can register a new user (201 Created)")
    void adminCanRegister() throws Exception {
        RegisterResponse response = new RegisterResponse(
                "00000000-0000-0000-0000-000000000001",
                "New User", "new@worklog.test", "2026-07-15T10:00");
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/worklog/auth/register")
                        .with(user("admin@worklog.test").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isCreated());

        verify(authService).register(any(RegisterRequest.class));
    }
}
