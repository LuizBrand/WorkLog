package br.com.luizbrand.worklog.tickets;

import br.com.luizbrand.worklog.exception.NotFound.TicketNotFoundException;
import br.com.luizbrand.worklog.tickets.dto.TicketLogResponse;
import br.com.luizbrand.worklog.tickets.enums.FieldType;
import br.com.luizbrand.worklog.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class TicketLogManager {

    private final TicketLogRepository ticketLogRepository;
    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;

    public TicketLogManager(TicketLogRepository ticketLogRepository,
                            TicketRepository ticketRepository,
                            TicketMapper ticketMapper) {
        this.ticketLogRepository = ticketLogRepository;
        this.ticketRepository = ticketRepository;
        this.ticketMapper = ticketMapper;
    }

    protected void generateLogs(Ticket oldTicket, Ticket newTicket, User currentUser) {
        UUID changeGroupId = UUID.randomUUID();
        List<TicketLog> logs = new ArrayList<>();

        checkChange(oldTicket, newTicket, oldTicket.getTitle(), newTicket.getTitle(), "title", FieldType.STRING, changeGroupId, currentUser, logs);
        checkChange(oldTicket, newTicket, oldTicket.getDescription(), newTicket.getDescription(), "description", FieldType.STRING, changeGroupId, currentUser, logs);
        checkChange(oldTicket, newTicket, oldTicket.getSolution(), newTicket.getSolution(), "solution", FieldType.STRING, changeGroupId, currentUser, logs);
        checkChange(oldTicket, newTicket, oldTicket.getStatus(), newTicket.getStatus(), "status", FieldType.STRING, changeGroupId, currentUser, logs);
        checkChange(oldTicket, newTicket, oldTicket.getCompletedAt(), newTicket.getCompletedAt(), "Conclusion Date", FieldType.DATETIME, changeGroupId, currentUser, logs);

        if (!logs.isEmpty()) {
            ticketLogRepository.saveAll(logs);
        }
    }

    public Page<TicketLogResponse> findLogsByTicket(UUID ticketPublicId, Pageable pageable) {
        ticketRepository.findByPublicId(ticketPublicId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with publicId: " + ticketPublicId));

        return ticketLogRepository
                .findByTicket_PublicIdOrderByChangeDateDesc(ticketPublicId, pageable)
                .map(ticketMapper::toLogResponse);
    }

    private void checkChange(Ticket associatedticket, Ticket newTicket,
                             Object oldValue, Object newValue,
                             String fieldName, FieldType fieldType,
                             UUID changeGroupId, User user, List<TicketLog> logs) {

        if (Objects.equals(oldValue, newValue )) {
            return;
        }
        String oldString = oldValue != null ? String.valueOf(oldValue) : null;
        String newString = newValue != null ? String.valueOf(newValue) : null;

        TicketLog log = TicketLog.builder()
                .ticket(associatedticket)
                .user(user)
                .client(newTicket.getClient())
                .system(newTicket.getSystem())
                .changeGroupId(changeGroupId)
                .fieldChanged(fieldName)
                .fieldType(fieldType)
                .oldValue(oldString)
                .newValue(newString)
                .changeDate(LocalDateTime.now())
                .build();

        logs.add(log);
    }

}
