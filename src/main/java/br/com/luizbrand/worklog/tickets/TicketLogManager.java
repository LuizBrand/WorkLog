package br.com.luizbrand.worklog.tickets;

import br.com.luizbrand.worklog.tickets.enums.FieldType;
import br.com.luizbrand.worklog.user.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class TicketLogManager {

    private final TicketLogRepository ticketLogRepository;

    public TicketLogManager(TicketLogRepository ticketLogRepository) {
        this.ticketLogRepository = ticketLogRepository;
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
                .newValeu(newString)
                .changeDate(LocalDateTime.now())
                .build();

        logs.add(log);
    }

}
