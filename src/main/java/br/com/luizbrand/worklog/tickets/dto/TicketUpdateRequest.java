package br.com.luizbrand.worklog.tickets.dto;

import br.com.luizbrand.worklog.tickets.enums.TicketStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TicketUpdateRequest(
        String title,
        String description,
        String solution,
        TicketStatus status,
        LocalDateTime completedAt,
        UUID userId
) {
}
