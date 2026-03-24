package br.com.luizbrand.worklog.auth;

import br.com.luizbrand.worklog.user.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private final TokenProperties tokenProperties;
    private final String secret;
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    public JwtService(TokenProperties tokenProperties) {
        this.tokenProperties = tokenProperties;
        this.secret = tokenProperties.getSecretKey();
    }

    //add claims e assina
    public String generateAcessToken(UserDetails user) {

        Algorithm algorithm = Algorithm.HMAC256(secret);

        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return JWT.create()
                .withIssuer("WorkLog App")
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(Instant.now())
                .withSubject(user.getUsername())
                .withClaim("roles", roles)
                .withExpiresAt(Instant.now().plusMillis(tokenProperties.getExpiration()))
                .sign(algorithm);
    }

    //Valid o token e retorna o subject
    public String extractUsername(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(tokenProperties.getSecretKey());

            return JWT.require(algorithm)
                    .withIssuer("WorkLog App")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException ex) {
            logger.warn("JWT verification failed: {}", ex.getMessage());
            return null;
        }
    }

    public boolean isTokenValid(String token, User user) {
        final String username = extractUsername(token);

        return username != null && username.equals(user.getUsername());
    }

}
