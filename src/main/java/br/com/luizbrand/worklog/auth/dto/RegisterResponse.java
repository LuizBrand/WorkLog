package br.com.luizbrand.worklog.auth.dto;

public record RegisterResponse(
        String publicId,
        String name,
        String email,
        String createdAt
) {
}
