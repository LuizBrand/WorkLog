package br.com.luizbrand.worklog.dto.response;

import java.util.UUID;

public record SystemResponse(
        UUID publicId,
        String name
) {
}
