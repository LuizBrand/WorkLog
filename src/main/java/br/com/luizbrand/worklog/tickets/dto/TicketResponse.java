package br.com.luizbrand.worklog.tickets.dto;

import br.com.luizbrand.worklog.client.dto.ClientResponse;
import br.com.luizbrand.worklog.client.dto.ClientSummary;
import br.com.luizbrand.worklog.system.dto.SystemResponse;
import br.com.luizbrand.worklog.tickets.enums.TicketStatus;
import br.com.luizbrand.worklog.user.dto.UserSummary;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record TicketResponse(
        UUID publicId,
        String title,
        String description,
        String solution,
        TicketStatus status,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        LocalDateTime updatedAt,
        ClientSummary client,
        SystemResponse system,
        UserSummary user
        ) {
}
