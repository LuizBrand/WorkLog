package br.com.luizbrand.worklog.auth;

public record AuthResponse(
        String publicId,
        String name,
        String email,
        String createdAt
) {
}
