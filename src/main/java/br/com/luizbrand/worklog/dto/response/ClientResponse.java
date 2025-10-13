package br.com.luizbrand.worklog.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ClientResponse(
        UUID publicId,
        String name,
        boolean enabled,
        OffsetDateTime createdAt,
        List<SystemResponse> systems) {
}
