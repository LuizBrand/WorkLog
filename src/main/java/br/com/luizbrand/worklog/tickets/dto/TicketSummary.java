package br.com.luizbrand.worklog.tickets.dto;

import br.com.luizbrand.worklog.client.dto.ClientSummary;
import br.com.luizbrand.worklog.system.dto.SystemResponse;
import br.com.luizbrand.worklog.tickets.enums.TicketStatus;
import br.com.luizbrand.worklog.user.dto.UserSummary;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record TicketSummary(
        UUID publicId,
        String title,
        String description,
        TicketStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt,
        ClientSummary client,
        SystemResponse system,
        UserSummary user
) {
}
