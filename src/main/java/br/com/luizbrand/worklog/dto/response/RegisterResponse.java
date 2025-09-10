package br.com.luizbrand.worklog.dto.response;

public record RegisterResponse(
        String publicId,
        String name,
        String email,
        String createdAt
) {
}
