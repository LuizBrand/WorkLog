package br.com.luizbrand.worklog.auth;

import br.com.luizbrand.worklog.user.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final String secret;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secret = jwtProperties.getSecretKey();
    }

    //add claims e assina
    public String generateAcessToken(User user) {

        Algorithm algorithm = Algorithm.HMAC256(secret);

        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .toList();

        return JWT.create()
                .withIssuer("WorkLog App")
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(Instant.now())
                .withSubject(user.getEmail())
                .withClaim("roles", roles)
                .withExpiresAt(Instant.now().plusMillis(jwtProperties.getExpiration()))
                .sign(algorithm);
    }

    //Gera token de duração maior
    public String generateRefreshToken(User user) {
        Algorithm algorithm = Algorithm.HMAC256(secret);

        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .toList();

        return JWT.create()
                .withIssuer("WorkLog App")
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(Instant.now())
                .withSubject(user.getEmail())
                .withClaim("roles", roles)
                .withExpiresAt(Instant.now().plusMillis(jwtProperties.getRefreshToken().getExpiration()))
                .sign(algorithm);
    }

    //Valid o token e retorna o subject
    private String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(jwtProperties.getSecretKey());

            return JWT.require(algorithm)
                    .withIssuer("WorkLog App")
                    .build()
                    .verify(token)
                    .getSignature();

        } catch (JWTVerificationException ex) {
            return "";
        }
    }

}
