package br.com.luizbrand.worklog.auth;

import br.com.luizbrand.worklog.support.JwtTestSupport;
import br.com.luizbrand.worklog.support.RoleTestBuilder;
import br.com.luizbrand.worklog.support.UserTestBuilder;
import br.com.luizbrand.worklog.user.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = JwtTestSupport.DEFAULT_SECRET;
    private static final long TTL_MILLIS = 60_000L;

    private JwtService jwtService;
    private TokenProperties tokenProperties;
    private User user;

    @BeforeEach
    void setUp() {
        tokenProperties = new TokenProperties();
        tokenProperties.setSecretKey(SECRET);
        tokenProperties.setExpiration(TTL_MILLIS);
        jwtService = new JwtService(tokenProperties);

        user = UserTestBuilder.aUser()
                .withEmail("jwt-user@worklog.test")
                .withRole(RoleTestBuilder.userRole())
                .build();
    }

    @Nested
    @DisplayName("Method: generateAcessToken()")
    class GenerateAccessTokenTests {

        @Test
        @DisplayName("Should issue a token with issuer, subject, roles and expiration derived from properties")
        void shouldIssueTokenWithExpectedClaims() {
            Instant before = Instant.now();

            String token = jwtService.generateAcessToken(user);
            DecodedJWT decoded = JWT.decode(token);

            assertThat(decoded.getIssuer()).isEqualTo("WorkLog App");
            assertThat(decoded.getSubject()).isEqualTo(user.getEmail());
            assertThat(decoded.getClaim("roles").asList(String.class)).containsExactly("ROLE_USER");
            assertThat(decoded.getId()).isNotBlank();
            assertThat(decoded.getExpiresAt().toInstant())
                    .isBetween(before.plusMillis(TTL_MILLIS - 1_000), before.plusMillis(TTL_MILLIS + 2_000));
        }
    }

    @Nested
    @DisplayName("Method: extractUsername()")
    class ExtractUsernameTests {

        @Test
        @DisplayName("Should return the subject for a valid token")
        void shouldReturnSubjectForValidToken() {
            String token = JwtTestSupport.validToken(SECRET, user.getEmail());

            String username = jwtService.extractUsername(token);

            assertThat(username).isEqualTo(user.getEmail());
        }

        @Test
        @DisplayName("Should return null when the token is expired")
        void shouldReturnNullForExpiredToken() {
            String token = JwtTestSupport.expiredToken(SECRET, user.getEmail());

            assertThat(jwtService.extractUsername(token)).isNull();
        }

        @Test
        @DisplayName("Should return null when the token is signed with a different secret")
        void shouldReturnNullForWrongSignature() {
            String token = JwtTestSupport.tokenSignedWith("different-secret-key", user.getEmail());

            assertThat(jwtService.extractUsername(token)).isNull();
        }

        @Test
        @DisplayName("Should return null when the token is malformed")
        void shouldReturnNullForMalformedToken() {
            assertThat(jwtService.extractUsername(JwtTestSupport.malformed())).isNull();
        }
    }

    @Nested
    @DisplayName("Method: isTokenValid()")
    class IsTokenValidTests {

        @Test
        @DisplayName("Should return true when the token's subject matches the user email")
        void shouldReturnTrueWhenSubjectMatches() {
            String token = JwtTestSupport.validToken(SECRET, user.getEmail());

            assertThat(jwtService.isTokenValid(token, user)).isTrue();
        }

        @Test
        @DisplayName("Should return false when the token's subject does not match")
        void shouldReturnFalseWhenSubjectDoesNotMatch() {
            String token = JwtTestSupport.validToken(SECRET, "someone-else@worklog.test");

            assertThat(jwtService.isTokenValid(token, user)).isFalse();
        }

        @Test
        @DisplayName("Should return false when the token is invalid")
        void shouldReturnFalseWhenTokenIsInvalid() {
            String token = JwtTestSupport.tokenSignedWith("different-secret", user.getEmail());

            assertThat(jwtService.isTokenValid(token, user)).isFalse();
        }
    }
}
