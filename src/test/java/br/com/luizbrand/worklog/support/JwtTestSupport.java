package br.com.luizbrand.worklog.support;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class JwtTestSupport {

    public static final String DEFAULT_SECRET = "test-secret-key-for-unit-tests-only-please";
    public static final String ISSUER = "WorkLog App";

    private JwtTestSupport() {
    }

    public static String validToken(String secret, String subject, List<String> roles, long ttlMillis) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.create()
                .withIssuer(ISSUER)
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(Instant.now())
                .withSubject(subject)
                .withClaim("roles", roles)
                .withExpiresAt(Instant.now().plusMillis(ttlMillis))
                .sign(algorithm);
    }

    public static String validToken(String secret, String subject) {
        return validToken(secret, subject, List.of("ROLE_USER"), 60_000L);
    }

    public static String expiredToken(String secret, String subject) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        Instant past = Instant.now().minusSeconds(3600);
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(subject)
                .withIssuedAt(past)
                .withExpiresAt(past.plusSeconds(60))
                .sign(algorithm);
    }

    public static String tokenSignedWith(String otherSecret, String subject) {
        Algorithm algorithm = Algorithm.HMAC256(otherSecret);
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(subject)
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusSeconds(60))
                .sign(algorithm);
    }

    public static String malformed() {
        return "not.a.valid.jwt.token";
    }
}
