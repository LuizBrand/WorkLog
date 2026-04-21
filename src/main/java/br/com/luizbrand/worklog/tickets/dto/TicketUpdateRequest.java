package br.com.luizbrand.worklog.tickets.dto;

import br.com.luizbrand.worklog.tickets.enums.TicketStatus;

import java.time.LocalDateTime;

public record TicketUpdateRequest(
        String title,
        String description,
        String solution,
        TicketStatus status,
        LocalDateTime completedAt
) {
}
