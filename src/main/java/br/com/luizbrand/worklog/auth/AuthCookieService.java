package br.com.luizbrand.worklog.auth;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthCookieService {

    private final CookieProperties cookieProperties;
    private final TokenProperties tokenProperties;

    public AuthCookieService(CookieProperties cookieProperties, TokenProperties tokenProperties) {
        this.cookieProperties = cookieProperties;
        this.tokenProperties = tokenProperties;
    }

    public ResponseCookie buildAccessCookie(String token) {
        return baseBuilder(cookieProperties.accessName(), token, "/")
                .maxAge(Duration.ofMillis(tokenProperties.getExpiration()))
                .build();
    }

    public ResponseCookie buildRefreshCookie(String token) {
        return baseBuilder(cookieProperties.refreshName(), token, cookieProperties.refreshPath())
                .maxAge(Duration.ofMillis(tokenProperties.getRefreshToken().getExpiration()))
                .build();
    }

    public ResponseCookie clearAccessCookie() {
        return baseBuilder(cookieProperties.accessName(), "", "/")
                .maxAge(0)
                .build();
    }

    public ResponseCookie clearRefreshCookie() {
        return baseBuilder(cookieProperties.refreshName(), "", cookieProperties.refreshPath())
                .maxAge(0)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseBuilder(String name, String value, String path) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(path);
    }
}
