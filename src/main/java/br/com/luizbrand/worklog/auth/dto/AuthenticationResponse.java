package br.com.luizbrand.worklog.auth.dto;

public record AuthenticationResponse(
        String acessToken,
        String refreshToken
) {
}
