package br.com.luizbrand.worklog.config.docs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    private final OpenAPI openAPI = new OpenApiConfig().openAPI();

    @Test
    @DisplayName("Should describe authentication as an HttpOnly cookie named worklog_access")
    void shouldDeclareCookieSecurityScheme() {
        Map<String, SecurityScheme> schemes = openAPI.getComponents().getSecuritySchemes();

        assertThat(schemes).hasSize(1);
        SecurityScheme scheme = schemes.values().iterator().next();

        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.APIKEY);
        assertThat(scheme.getIn()).isEqualTo(SecurityScheme.In.COOKIE);
        assertThat(scheme.getName()).isEqualTo("worklog_access");
    }

    @Test
    @DisplayName("Should reference the cookie scheme as the global security requirement")
    void shouldRequireTheCookieScheme() {
        String schemeName = openAPI.getComponents().getSecuritySchemes().keySet().iterator().next();

        assertThat(openAPI.getSecurity()).isNotEmpty();
        SecurityRequirement requirement = openAPI.getSecurity().get(0);
        assertThat(requirement).containsKey(schemeName);
    }

    @Test
    @DisplayName("Should not advertise the legacy Bearer JWT scheme")
    void shouldNotDeclareBearerScheme() {
        Map<String, SecurityScheme> schemes = openAPI.getComponents().getSecuritySchemes();

        assertThat(schemes.values())
                .noneMatch(s -> s.getType() == SecurityScheme.Type.HTTP
                        && "bearer".equalsIgnoreCase(s.getScheme()));
    }
}
