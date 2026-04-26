package br.com.luizbrand.worklog.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    private CorsConfigurationSource sourceFor(List<String> origins) {
        return new CorsConfig().corsConfigurationSource(new CorsProperties(origins));
    }

    private HttpServletRequest requestFor(String path, String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        if (origin != null) {
            request.addHeader("Origin", origin);
        }
        return request;
    }

    @Test
    @DisplayName("Should expose the configured allowed origins for any request path")
    void shouldExposeConfiguredOrigins() {
        CorsConfigurationSource source = sourceFor(List.of("http://localhost:3000"));

        CorsConfiguration cfg = source.getCorsConfiguration(requestFor("/tickets", "http://localhost:3000"));

        assertThat(cfg).isNotNull();
        assertThat(cfg.getAllowedOrigins()).containsExactly("http://localhost:3000");
    }

    @Test
    @DisplayName("Should allow the standard REST methods plus OPTIONS for preflight")
    void shouldAllowStandardMethods() {
        CorsConfigurationSource source = sourceFor(List.of("http://localhost:3000"));

        CorsConfiguration cfg = source.getCorsConfiguration(requestFor("/tickets", "http://localhost:3000"));

        assertThat(cfg).isNotNull();
        assertThat(cfg.getAllowedMethods())
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    }

    @Test
    @DisplayName("Should allow Authorization, Content-Type and Accept headers")
    void shouldAllowAuthHeaders() {
        CorsConfigurationSource source = sourceFor(List.of("http://localhost:3000"));

        CorsConfiguration cfg = source.getCorsConfiguration(requestFor("/tickets", "http://localhost:3000"));

        assertThat(cfg).isNotNull();
        assertThat(cfg.getAllowedHeaders())
                .contains("Authorization", "Content-Type", "Accept");
    }

    @Test
    @DisplayName("Should not enable allowCredentials so Bearer-token clients work without cookies")
    void shouldNotAllowCredentials() {
        CorsConfigurationSource source = sourceFor(List.of("http://localhost:3000"));

        CorsConfiguration cfg = source.getCorsConfiguration(requestFor("/tickets", "http://localhost:3000"));

        assertThat(cfg).isNotNull();
        assertThat(cfg.getAllowCredentials()).isNotEqualTo(Boolean.TRUE);
    }

    @Test
    @DisplayName("Should honor multiple configured origins so dev and staging can coexist")
    void shouldHonorMultipleOrigins() {
        CorsConfigurationSource source = sourceFor(List.of(
                "http://localhost:3000",
                "https://staging.worklog.test"));

        CorsConfiguration cfg = source.getCorsConfiguration(requestFor("/tickets", "http://localhost:3000"));

        assertThat(cfg).isNotNull();
        assertThat(cfg.getAllowedOrigins())
                .containsExactlyInAnyOrder("http://localhost:3000", "https://staging.worklog.test");
    }
}
