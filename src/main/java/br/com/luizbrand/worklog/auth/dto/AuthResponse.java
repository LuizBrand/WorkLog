package br.com.luizbrand.worklog.auth.dto;

public record AuthResponse(
        String publicId,
        String name,
        String email,
        String createdAt
) {
}
