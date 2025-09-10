package br.com.luizbrand.worklog.dto.response;

import br.com.luizbrand.worklog.enums.RoleName;

import java.util.Set;

public record UserResponse(
        String publicId,
        String email,
        String name,
        Set<RoleResponse> roles) {
}
