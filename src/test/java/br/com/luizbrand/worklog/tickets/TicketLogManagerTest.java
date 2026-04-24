package br.com.luizbrand.worklog.tickets;

import br.com.luizbrand.worklog.client.Client;
import br.com.luizbrand.worklog.exception.NotFound.TicketNotFoundException;
import br.com.luizbrand.worklog.support.ClientTestBuilder;
import br.com.luizbrand.worklog.support.SystemTestBuilder;
import br.com.luizbrand.worklog.support.TicketTestBuilder;
import br.com.luizbrand.worklog.support.UserTestBuilder;
import br.com.luizbrand.worklog.system.Systems;
import br.com.luizbrand.worklog.tickets.dto.TicketLogResponse;
import br.com.luizbrand.worklog.tickets.enums.FieldType;
import br.com.luizbrand.worklog.tickets.enums.TicketStatus;
import br.com.luizbrand.worklog.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketLogManagerTest {

    @Mock
    private TicketLogRepository ticketLogRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketMapper ticketMapper;

    @InjectMocks
    private TicketLogManager ticketLogManager;

    private User currentUser;
    private Client client;
    private Systems system;

    @BeforeEach
    void setUp() {
        currentUser = UserTestBuilder.aUser().withEmail("editor@worklog.test").build();
        client = ClientTestBuilder.aClient().build();
        system = SystemTestBuilder.aSystem().build();
    }

    private Ticket baseTicket() {
        return TicketTestBuilder.aTicket()
                .withTitle("Old title")
                .withDescription("Old description")
                .withSolution(null)
                .withStatus(TicketStatus.PENDING)
                .withCompletedAt(null)
                .withClient(client)
                .withSystem(system)
                .withUser(currentUser)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<TicketLog> capturedLogs() {
        ArgumentCaptor<List<TicketLog>> captor = ArgumentCaptor.forClass(List.class);
        verify(ticketLogRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("Method: generateLogs() — single field changes")
    class SingleFieldChangeTests {

        @Test
        @DisplayName("Should log a STRING entry when the title changes")
        void shouldLogTitleChange() {
            Ticket oldTicket = baseTicket();
            Ticket newTicket = baseTicket();
            newTicket.setTitle("New title");

            ticketLogManager.generateLogs(oldTicket, newTicket, currentUser);

            List<TicketLog> logs = capturedLogs();
            assertThat(logs).hasSize(1);
            TicketLog log = logs.get(0);
            assertThat(log.getFieldChanged()).isEqualTo("title");
            assertThat(log.getFieldType()).isEqualTo(FieldType.STRING);
            assertThat(log.getOldValue()).isEqualTo("Old title");
            assertThat(log.getNewValue()).isEqualTo("New title");
            assertThat(log.getUser()).isEqualTo(currentUser);
            assertThat(log.getTicket()).isSameAs(oldTicket);
            assertThat(log.getClient()).isEqualTo(client);
            assertThat(log.getSystem()).isEqualTo(system);
            assertThat(log.getChangeGroupId()).isNotNull();
            assertThat(log.getChangeDate()).isNotNull();
        }

        @Test
        @DisplayName("Should log a STRING entry when the description changes")
        void shouldLogDescriptionChange() {
            Ticket oldTicket = baseTicket();
            Ticket newTicket = baseTicket();
            newTicket.setDescription("New description");

            ticketLogManager.generateLogs(oldTicket, newTicket, currentUser);

            List<TicketLog> logs = capturedLogs();
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getFieldChanged()).isEqualTo("description");
            assertThat(logs.get(0).getFieldType()).isEqualTo(FieldType.STRING);
            assertThat(logs.get(0).getOldValue()).isEqualTo("Old description");
            assertThat(logs.get(0).getNewValue()).isEqualTo("New description");
        }

        @Test
        @DisplayName("Should log a STRING entry when the solution changes from null to a value")
        void shouldLogSolutionFromNullToValue() {
            Ticket oldTicket = baseTicket();
            Ticket newTicket = baseTicket();
            newTicket.setSolution("Applied a fix");

            ticketLogManager.generateLogs(oldTicket, newTicket, currentUser);

            List<TicketLog> logs = capturedLogs();
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getFieldChanged()).isEqualTo("solution");
            assertThat(logs.get(0).getOldValue()).isNull();
            assertThat(logs.get(0).getNewValue()).isEqualTo("Applied a fix");
        }

        @Test
        @DisplayName("Should log a STRING entry when the solution changes from a value to null")
        void shouldLogSolutionFromValueToNull() {
            Ticket oldTicket = baseTicket();
            oldTicket.setSolution("Previous fix");
            Ticket newTicket = baseTicket();
            newTicket.setSolution(null);

            ticketLogManager.generateLogs(oldTicket, newTicket, currentUser);

            List<TicketLog> logs = capturedLogs();
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getFieldChanged()).isEqualTo("solution");
            assertThat(logs.get(0).getOldValue()).isEqualTo("Previous fix");
            assertThat(logs.get(0).getNewValue()).isNull();
        }

        @Test
        @DisplayName("Should log a STRING entry when the status changes")
        void shouldLogStatusChange() {
            Ticket oldTicket = baseTicket();
            Ticket newTicket = baseTicket();
            newTicket.setStatus(TicketStatus.COMPLETED);

            ticketLogManager.generateLogs(oldTicket, newTicket, currentUser);

            List<TicketLog> logs = capturedLogs();
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getFieldChanged()).isEqualTo("status");
            assertThat(logs.get(0).getFieldType()).isEqualTo(FieldType.STRING);
            assertThat(logs.get(0).getOldValue()).isEqualTo(TicketStatus.PENDING.name());
            assertThat(logs.get(0).getNewValue()).isEqualTo(TicketStatus.COMPLETED.name());
        }

        @Test
        @DisplayName("Should log a DATETIME entry with label 'Conclusion Date' when completedAt changes")
        void shouldLogCompletedAtChange() {
            LocalDateTime completion = LocalDateTime.of(2026, 4, 21, 12, 0);
            Ticket oldTicket = baseTicket();
            Ticket newTicket = baseTicket();
            newTicket.setCompletedAt(completion);

            ticketLogManager.generateLogs(oldTicket, newTicket, currentUser);

            List<TicketLog> logs = capturedLogs();
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getFieldChanged()).isEqualTo("Conclusion Date");
            assertThat(logs.get(0).getFieldType()).isEqualTo(FieldType.DATETIME);
            assertThat(logs.get(0).getOldValue()).isNull();
            assertThat(logs.get(0).getNewValue()).isEqualTo(completion.toString());
        }
    }

    @Nested
    @DisplayName("Method: generateLogs() — no change")
    class NoChangeTests {

        @Test
        @DisplayName("Should not save any log when nothing changed")
        void shouldNotSaveLogsWhenNothingChanged() {
            Ticket oldTicket = baseTicket();
            Ticket newTicket = baseTicket();

            ticketLogManager.generateLogs(oldTicket, newTicket, currentUser);

            verify(ticketLogRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
        }

        @Test
        @DisplayName("Should not log a field when both old and new values are null")
        void shouldNotLogWhenBothSidesAreNull() {
            Ticket oldTicket = baseTicket();
            oldTicket.setSolution(null);
            oldTicket.setCompletedAt(null);
            Ticket newTicket = baseTicket();
            newTicket.setSolution(null);
            newTicket.setCompletedAt(null);

            ticketLogManager.generateLogs(oldTicket, newTicket, currentUser);

            verify(ticketLogRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
        }
    }

    @Nested
    @DisplayName("Method: generateLogs() — multiple field changes")
    class MultipleChangeTests {

        @Test
        @DisplayName("Should share the same changeGroupId across all logs produced in a single call")
        void shouldShareChangeGroupIdAcrossAllLogs() {
            Ticket oldTicket = baseTicket();
            Ticket newTicket = baseTicket();
            newTicket.setTitle("New title");
            newTicket.setDescription("New description");
            newTicket.setSolution("Applied a fix");
            newTicket.setStatus(TicketStatus.COMPLETED);
            newTicket.setCompletedAt(LocalDateTime.of(2026, 4, 21, 12, 0));

            ticketLogManager.generateLogs(oldTicket, newTicket, currentUser);

            List<TicketLog> logs = capturedLogs();
            assertThat(logs).hasSize(5);

            UUID groupId = logs.get(0).getChangeGroupId();
            assertThat(groupId).isNotNull();
            assertThat(logs).extracting(TicketLog::getChangeGroupId).containsOnly(groupId);

            assertThat(logs).extracting(TicketLog::getFieldChanged)
                    .containsExactlyInAnyOrder(
                            "title", "description", "solution", "status", "Conclusion Date");
        }

        @Test
        @DisplayName("Should persist logs exactly once via saveAll")
        void shouldInvokeSaveAllOnlyOnce() {
            Ticket oldTicket = baseTicket();
            Ticket newTicket = baseTicket();
            newTicket.setTitle("New title");
            newTicket.setStatus(TicketStatus.COMPLETED);

            ticketLogManager.generateLogs(oldTicket, newTicket, currentUser);

            verify(ticketLogRepository, times(1)).saveAll(org.mockito.ArgumentMatchers.anyList());
        }
    }

    @Nested
    @DisplayName("Method: findLogsByTicket()")
    class FindLogsByTicketTests {

        @Test
        @DisplayName("Should throw TicketNotFoundException when the ticket does not exist")
        void shouldThrowWhenTicketMissing() {
            UUID missing = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 20);
            when(ticketRepository.findByPublicId(missing)).thenReturn(Optional.empty());

            TicketNotFoundException ex = assertThrows(TicketNotFoundException.class,
                    () -> ticketLogManager.findLogsByTicket(missing, pageable));

            assertThat(ex.getMessage()).contains(missing.toString());
            verifyNoInteractions(ticketLogRepository);
            verifyNoInteractions(ticketMapper);
        }

        @Test
        @DisplayName("Should return a page of mapped log responses ordered by changeDate desc")
        void shouldReturnMappedPage() {
            Ticket ticket = TicketTestBuilder.aTicket()
                    .withClient(client).withSystem(system).withUser(currentUser)
                    .build();
            Pageable pageable = PageRequest.of(0, 20);

            TicketLog firstLog = TicketLog.builder()
                    .id(1L)
                    .changeGroupId(UUID.randomUUID())
                    .fieldChanged("title")
                    .fieldType(FieldType.STRING)
                    .oldValue("Old").newValue("New")
                    .changeDate(LocalDateTime.of(2026, 4, 22, 10, 0))
                    .user(currentUser)
                    .ticket(ticket)
                    .build();
            TicketLog secondLog = TicketLog.builder()
                    .id(2L)
                    .changeGroupId(UUID.randomUUID())
                    .fieldChanged("status")
                    .fieldType(FieldType.STRING)
                    .oldValue("PENDING").newValue("COMPLETED")
                    .changeDate(LocalDateTime.of(2026, 4, 21, 9, 0))
                    .user(currentUser)
                    .ticket(ticket)
                    .build();

            TicketLogResponse firstResponse = TicketLogResponse.builder()
                    .changeGroupId(firstLog.getChangeGroupId()).fieldChanged("title").build();
            TicketLogResponse secondResponse = TicketLogResponse.builder()
                    .changeGroupId(secondLog.getChangeGroupId()).fieldChanged("status").build();

            when(ticketRepository.findByPublicId(ticket.getPublicId())).thenReturn(Optional.of(ticket));
            Page<TicketLog> repoPage = new PageImpl<>(List.of(firstLog, secondLog), pageable, 2);
            when(ticketLogRepository.findByTicket_PublicIdOrderByChangeDateDesc(ticket.getPublicId(), pageable))
                    .thenReturn(repoPage);
            when(ticketMapper.toLogResponse(firstLog)).thenReturn(firstResponse);
            when(ticketMapper.toLogResponse(secondLog)).thenReturn(secondResponse);

            Page<TicketLogResponse> result = ticketLogManager.findLogsByTicket(ticket.getPublicId(), pageable);

            assertThat(result.getContent()).containsExactly(firstResponse, secondResponse);
            assertThat(result.getTotalElements()).isEqualTo(2);
            verify(ticketLogRepository, times(1))
                    .findByTicket_PublicIdOrderByChangeDateDesc(eq(ticket.getPublicId()), eq(pageable));
            verify(ticketMapper, times(1)).toLogResponse(firstLog);
            verify(ticketMapper, times(1)).toLogResponse(secondLog);
        }

        @Test
        @DisplayName("Should return an empty page when the ticket has no logs")
        void shouldReturnEmptyPage() {
            Ticket ticket = TicketTestBuilder.aTicket()
                    .withClient(client).withSystem(system).withUser(currentUser)
                    .build();
            Pageable pageable = PageRequest.of(0, 20);

            when(ticketRepository.findByPublicId(ticket.getPublicId())).thenReturn(Optional.of(ticket));
            when(ticketLogRepository.findByTicket_PublicIdOrderByChangeDateDesc(ticket.getPublicId(), pageable))
                    .thenReturn(Page.empty(pageable));

            Page<TicketLogResponse> result = ticketLogManager.findLogsByTicket(ticket.getPublicId(), pageable);

            assertThat(result.getContent()).isEmpty();
            verify(ticketMapper, never()).toLogResponse(org.mockito.ArgumentMatchers.any());
        }
    }
}
