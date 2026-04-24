package br.com.luizbrand.worklog.tickets.dto;

import br.com.luizbrand.worklog.tickets.enums.FieldType;
import br.com.luizbrand.worklog.user.dto.UserSummary;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record TicketLogResponse(
        UUID changeGroupId,
        String fieldChanged,
        FieldType fieldType,
        String oldValue,
        String newValue,
        LocalDateTime changeDate,
        UserSummary user
) {
}
