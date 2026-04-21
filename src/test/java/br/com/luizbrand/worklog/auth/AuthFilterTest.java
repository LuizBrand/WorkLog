package br.com.luizbrand.worklog.auth;

import br.com.luizbrand.worklog.support.JwtTestSupport;
import br.com.luizbrand.worklog.support.RoleTestBuilder;
import br.com.luizbrand.worklog.support.UserTestBuilder;
import br.com.luizbrand.worklog.user.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private AuthFilter authFilter;

    private User user;

    @BeforeEach
    void setUp() {
        user = UserTestBuilder.aUser()
                .withEmail("filter-user@worklog.test")
                .withRole(RoleTestBuilder.userRole())
                .build();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Method: doFilterInternal()")
    class DoFilterInternalTests {

        @Test
        @DisplayName("Should pass through without authenticating when Authorization header is missing")
        void shouldPassThroughWhenHeaderMissing() throws Exception {
            HttpServletRequest request = new MockHttpServletRequest();
            HttpServletResponse response = new MockHttpServletResponse();

            authFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain, times(1)).doFilter(request, response);
            verify(jwtService, never()).extractUsername(any());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Should pass through without authenticating when header does not start with Bearer")
        void shouldPassThroughWhenHeaderDoesNotStartWithBearer() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
            HttpServletResponse response = new MockHttpServletResponse();

            authFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain, times(1)).doFilter(request, response);
            verify(jwtService, never()).extractUsername(any());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Should populate the SecurityContext when Bearer token is valid")
        void shouldAuthenticateWhenBearerTokenValid() throws Exception {
            String token = "valid-token";
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + token);
            HttpServletResponse response = new MockHttpServletResponse();

            when(jwtService.extractUsername(token)).thenReturn(user.getEmail());
            when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(user);
            when(jwtService.isTokenValid(token, user)).thenReturn(true);

            authFilter.doFilterInternal(request, response, filterChain);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.getPrincipal()).isEqualTo(user);
            assertThat(authentication.getAuthorities())
                    .extracting(org.springframework.security.core.GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
            verify(filterChain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("Should continue the chain without setting auth when token is invalid")
        void shouldNotAuthenticateWhenTokenInvalid() throws Exception {
            String token = JwtTestSupport.malformed();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + token);
            HttpServletResponse response = new MockHttpServletResponse();

            when(jwtService.extractUsername(token)).thenReturn(null);

            authFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(userDetailsService, never()).loadUserByUsername(any());
            verify(filterChain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("Should not set auth when isTokenValid returns false")
        void shouldNotAuthenticateWhenIsTokenValidFalse() throws Exception {
            String token = "tampered-token";
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + token);
            HttpServletResponse response = new MockHttpServletResponse();

            when(jwtService.extractUsername(token)).thenReturn(user.getEmail());
            when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(user);
            when(jwtService.isTokenValid(token, user)).thenReturn(false);

            authFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain, times(1)).doFilter(request, response);
        }
    }
}
