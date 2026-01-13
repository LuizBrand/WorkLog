package br.com.luizbrand.worklog.client.dto;

import java.util.UUID;

public record ClientSummary(
        UUID publicId,
        String name,
        boolean enabled
) {
}
