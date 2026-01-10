package br.com.luizbrand.worklog.system.dto;

import java.util.UUID;

public record SystemResponse(
        UUID publicId,
        String name
) {
}
