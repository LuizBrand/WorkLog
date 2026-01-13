package br.com.luizbrand.worklog.tickets.dto;

import br.com.luizbrand.worklog.tickets.enums.TicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record TicketRequest(
        @NotBlank(message = "O título é obrigatório")
        String title,
        @NotBlank(message = "A descrição é obrigatória")
        String description,
        String solution,
        @NotNull(message = "O status é obrigatório")
        TicketStatus status,
        LocalDateTime completedAt,
        @NotNull(message = "O cliente é obrigatório")
        UUID clientId,
        @NotNull(message = "O sistema é obrigatório")
        UUID systemId,
        UUID userId
        ) {
}
