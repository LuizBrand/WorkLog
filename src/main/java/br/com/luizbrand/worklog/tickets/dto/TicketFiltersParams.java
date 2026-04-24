package br.com.luizbrand.worklog.tickets.dto;

import br.com.luizbrand.worklog.tickets.enums.TicketStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

public record TicketFiltersParams(
        String title,
        TicketStatus status,
        UUID clientId,
        UUID systemId,
        UUID userId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo
) {
}
