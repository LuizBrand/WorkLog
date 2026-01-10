package br.com.luizbrand.worklog.system;

import java.util.UUID;

public record SystemResponse(
        UUID publicId,
        String name
) {
}
