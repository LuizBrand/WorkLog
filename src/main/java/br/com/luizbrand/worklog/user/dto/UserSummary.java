package br.com.luizbrand.worklog.user.dto;

import java.util.UUID;

public record UserSummary(
        UUID publicId,
        String name,
        String email
) {
}
