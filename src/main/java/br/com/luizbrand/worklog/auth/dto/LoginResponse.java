package br.com.luizbrand.worklog.auth.dto;

public record LoginResponse(
        String acessToken,
        String refreshToken
) {
}
