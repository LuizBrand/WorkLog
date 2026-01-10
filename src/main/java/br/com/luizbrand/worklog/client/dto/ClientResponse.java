package br.com.luizbrand.worklog.client.dto;

import br.com.luizbrand.worklog.system.dto.SystemResponse;
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
