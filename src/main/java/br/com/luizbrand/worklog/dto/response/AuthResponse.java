package br.com.luizbrand.worklog.dto.response;

public record AuthResponse(
        String publicId,
        String name,
        String email,
        String createdAt
) {
}
