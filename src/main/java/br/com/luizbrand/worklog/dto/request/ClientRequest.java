package br.com.luizbrand.worklog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ClientRequest(
        @NotBlank(message = "O nome é obrigatório")
        String name,
        @NotEmpty(message = "A lista de sistemas não pode estar vazia")
        List<UUID> systemsPublicIds) {
}
