package br.com.luizbrand.worklog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ClientRequest(
        @NotBlank(message = "O nome é obrigatório")
        String name,
        @NotNull(message = "O ID do sistema é obrigatório")
        List<UUID> systemsPublicIds) {
}
