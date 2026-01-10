package br.com.luizbrand.worklog.user.dto;

import br.com.luizbrand.worklog.role.dto.RoleResponse;
import java.util.Set;

public record UserResponse(
        String publicId,
        String email,
        String name,
        Set<RoleResponse> roles,
        String createdAt) {
}
