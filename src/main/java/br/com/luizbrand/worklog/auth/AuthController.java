package br.com.luizbrand.worklog.auth;

import br.com.luizbrand.worklog.auth.dto.LoginRequest;
import br.com.luizbrand.worklog.auth.dto.RegisterRequest;
import br.com.luizbrand.worklog.auth.dto.RegisterResponse;
import br.com.luizbrand.worklog.exception.Business.RefreshTokenException;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/worklog/auth")
public class AuthController implements AuthControllerDocs {

    private final AuthService authService;
    private final AuthCookieService authCookieService;

    public AuthController(AuthService authService, AuthCookieService authCookieService) {
        this.authService = authService;
        this.authCookieService = authCookieService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody @Valid RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody LoginRequest login) {
        AuthTokens tokens = authService.login(login);
        return tokenCookiesResponse(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshToken(
            @CookieValue(name = "worklog_refresh", required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new RefreshTokenException("Refresh token cookie is missing.");
        }
        AuthTokens tokens = authService.refreshToken(refreshToken);
        return tokenCookiesResponse(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "worklog_refresh", required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieService.clearAccessCookie().toString())
                .header(HttpHeaders.SET_COOKIE, authCookieService.clearRefreshCookie().toString())
                .build();
    }

    private ResponseEntity<Void> tokenCookiesResponse(AuthTokens tokens) {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieService.buildAccessCookie(tokens.accessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, authCookieService.buildRefreshCookie(tokens.refreshToken()).toString())
                .build();
    }

}
