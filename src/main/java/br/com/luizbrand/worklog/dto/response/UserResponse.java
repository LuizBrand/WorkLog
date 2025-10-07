package br.com.luizbrand.worklog.dto.response;

import java.util.Set;

public record UserResponse(
        String publicId,
        String email,
        String name,
        Set<RoleResponse> roles,
        String createdAt) {
}
