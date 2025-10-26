package br.com.luizbrand.worklog.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SystemRequest(
        @NotBlank(message = "O nome é obrigatório")
        String name
) {
}
