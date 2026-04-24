package br.com.luizbrand.worklog.tickets;

import br.com.luizbrand.worklog.client.Client;
import br.com.luizbrand.worklog.client.ClientMapper;
import br.com.luizbrand.worklog.client.dto.ClientSummary;
import br.com.luizbrand.worklog.support.ClientTestBuilder;
import br.com.luizbrand.worklog.support.SystemTestBuilder;
import br.com.luizbrand.worklog.support.TicketTestBuilder;
import br.com.luizbrand.worklog.support.UserTestBuilder;
import br.com.luizbrand.worklog.system.SystemMapper;
import br.com.luizbrand.worklog.system.Systems;
import br.com.luizbrand.worklog.system.dto.SystemResponse;
import br.com.luizbrand.worklog.tickets.dto.TicketLogResponse;
import br.com.luizbrand.worklog.tickets.dto.TicketRequest;
import br.com.luizbrand.worklog.tickets.dto.TicketResponse;
import br.com.luizbrand.worklog.tickets.dto.TicketSummary;
import br.com.luizbrand.worklog.tickets.enums.FieldType;
import br.com.luizbrand.worklog.tickets.enums.TicketStatus;
import br.com.luizbrand.worklog.user.User;
import br.com.luizbrand.worklog.user.UserMapper;
import br.com.luizbrand.worklog.user.dto.UserSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketMapperTest {

    @Mock
    private ClientMapper clientMapper;

    @Mock
    private SystemMapper systemMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private TicketMapper ticketMapper = Mappers.getMapper(TicketMapper.class);

    private Client client;
    private Systems system;
    private User user;

    @BeforeEach
    void setUp() {
        client = ClientTestBuilder.aClient().withName("Acme").build();
        system = SystemTestBuilder.aSystem().withName("Billing").build();
        user = UserTestBuilder.aUser().withName("Assignee").withEmail("assignee@worklog.test").build();
    }

    @Nested
    @DisplayName("Method: toEntity()")
    class ToEntityTests {

        @Test
        @DisplayName("Should map request fields and leave id, publicId, timestamps and relations null")
        void shouldMapRequestIgnoringIdentitiesAndRelations() {
            TicketRequest request = new TicketRequest(
                    "Investigate login bug",
                    "Users cannot log in after password reset.",
                    null,
                    TicketStatus.PENDING,
                    null,
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

            Ticket ticket = ticketMapper.toEntity(request);

            assertThat(ticket).isNotNull();
            assertThat(ticket.getTitle()).isEqualTo(request.title());
            assertThat(ticket.getDescription()).isEqualTo(request.description());
            assertThat(ticket.getSolution()).isNull();
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.PENDING);
            assertThat(ticket.getCompletedAt()).isNull();
            assertThat(ticket.getId()).isNull();
            assertThat(ticket.getPublicId()).isNull();
            assertThat(ticket.getCreatedAt()).isNull();
            assertThat(ticket.getUpdatedAt()).isNull();
            assertThat(ticket.getClient()).isNull();
            assertThat(ticket.getSystem()).isNull();
            assertThat(ticket.getUser()).isNull();
        }
    }

    @Nested
    @DisplayName("Method: toResponse()")
    class ToResponseTests {

        @Test
        @DisplayName("Should map the ticket and delegate nested mapping to Client, System and User mappers")
        void shouldMapTicketToResponseWithSummaries() {
            LocalDateTime completion = LocalDateTime.of(2026, 4, 21, 12, 0);
            Ticket ticket = TicketTestBuilder.aTicket()
                    .withTitle("Ticket X")
                    .withDescription("Desc X")
                    .withSolution("Sol X")
                    .withStatus(TicketStatus.COMPLETED)
                    .withCompletedAt(completion)
                    .withClient(client).withSystem(system).withUser(user)
                    .build();

            ClientSummary clientSummary = new ClientSummary(client.getPublicId(), client.getName(), true);
            SystemResponse systemResponse = new SystemResponse(system.getPublicId(), system.getName());
            UserSummary userSummary = new UserSummary(user.getPublicId(), user.getName(), user.getEmail());

            when(clientMapper.toSummary(client)).thenReturn(clientSummary);
            when(systemMapper.toSystemResponse(system)).thenReturn(systemResponse);
            when(userMapper.toUserSummary(user)).thenReturn(userSummary);

            TicketResponse response = ticketMapper.toResponse(ticket);

            assertThat(response.publicId()).isEqualTo(ticket.getPublicId());
            assertThat(response.title()).isEqualTo("Ticket X");
            assertThat(response.description()).isEqualTo("Desc X");
            assertThat(response.solution()).isEqualTo("Sol X");
            assertThat(response.status()).isEqualTo(TicketStatus.COMPLETED);
            assertThat(response.completedAt()).isEqualTo(completion);
            assertThat(response.createdAt()).isEqualTo(ticket.getCreatedAt());
            assertThat(response.updatedAt()).isEqualTo(ticket.getUpdatedAt());
            assertThat(response.client()).isEqualTo(clientSummary);
            assertThat(response.system()).isEqualTo(systemResponse);
            assertThat(response.user()).isEqualTo(userSummary);

            verify(clientMapper, times(1)).toSummary(client);
            verify(systemMapper, times(1)).toSystemResponse(system);
            verify(userMapper, times(1)).toUserSummary(user);
        }

        @Test
        @DisplayName("Should return null when the ticket is null")
        void shouldReturnNullForNullTicket() {
            assertThat(ticketMapper.toResponse(null)).isNull();
        }
    }

    @Nested
    @DisplayName("Method: toSummary()")
    class ToSummaryTests {

        @Test
        @DisplayName("Should map the ticket to a summary without solution and delegate nested mapping")
        void shouldMapTicketToSummaryWithoutSolution() {
            LocalDateTime completion = LocalDateTime.of(2026, 4, 21, 12, 0);
            Ticket ticket = TicketTestBuilder.aTicket()
                    .withTitle("Ticket X")
                    .withDescription("Desc X")
                    .withSolution("Sol X")
                    .withStatus(TicketStatus.COMPLETED)
                    .withCompletedAt(completion)
                    .withClient(client).withSystem(system).withUser(user)
                    .build();

            ClientSummary clientSummary = new ClientSummary(client.getPublicId(), client.getName(), true);
            SystemResponse systemResponse = new SystemResponse(system.getPublicId(), system.getName());
            UserSummary userSummary = new UserSummary(user.getPublicId(), user.getName(), user.getEmail());

            when(clientMapper.toSummary(client)).thenReturn(clientSummary);
            when(systemMapper.toSystemResponse(system)).thenReturn(systemResponse);
            when(userMapper.toUserSummary(user)).thenReturn(userSummary);

            TicketSummary summary = ticketMapper.toSummary(ticket);

            assertThat(summary.publicId()).isEqualTo(ticket.getPublicId());
            assertThat(summary.title()).isEqualTo("Ticket X");
            assertThat(summary.description()).isEqualTo("Desc X");
            assertThat(summary.status()).isEqualTo(TicketStatus.COMPLETED);
            assertThat(summary.createdAt()).isEqualTo(ticket.getCreatedAt());
            assertThat(summary.updatedAt()).isEqualTo(ticket.getUpdatedAt());
            assertThat(summary.completedAt()).isEqualTo(completion);
            assertThat(summary.client()).isEqualTo(clientSummary);
            assertThat(summary.system()).isEqualTo(systemResponse);
            assertThat(summary.user()).isEqualTo(userSummary);

            verify(clientMapper, times(1)).toSummary(client);
            verify(systemMapper, times(1)).toSystemResponse(system);
            verify(userMapper, times(1)).toUserSummary(user);
        }

        @Test
        @DisplayName("Should return null when the ticket is null")
        void shouldReturnNullForNullTicket() {
            assertThat(ticketMapper.toSummary(null)).isNull();
        }
    }

    @Nested
    @DisplayName("Method: toLogResponse()")
    class ToLogResponseTests {

        @Test
        @DisplayName("Should map every field and delegate the author to UserMapper")
        void shouldMapLogFieldsAndDelegateUser() {
            UUID changeGroupId = UUID.randomUUID();
            LocalDateTime changeDate = LocalDateTime.of(2026, 4, 21, 12, 0);
            TicketLog log = TicketLog.builder()
                    .id(42L)
                    .changeGroupId(changeGroupId)
                    .fieldChanged("title")
                    .fieldType(FieldType.STRING)
                    .oldValue("Old title")
                    .newValue("New title")
                    .changeDate(changeDate)
                    .user(user)
                    .build();

            UserSummary userSummary = new UserSummary(user.getPublicId(), user.getName(), user.getEmail());
            when(userMapper.toUserSummary(user)).thenReturn(userSummary);

            TicketLogResponse response = ticketMapper.toLogResponse(log);

            assertThat(response.changeGroupId()).isEqualTo(changeGroupId);
            assertThat(response.fieldChanged()).isEqualTo("title");
            assertThat(response.fieldType()).isEqualTo(FieldType.STRING);
            assertThat(response.oldValue()).isEqualTo("Old title");
            assertThat(response.newValue()).isEqualTo("New title");
            assertThat(response.changeDate()).isEqualTo(changeDate);
            assertThat(response.user()).isEqualTo(userSummary);

            verify(userMapper, times(1)).toUserSummary(user);
        }

        @Test
        @DisplayName("Should return null when the log is null")
        void shouldReturnNullForNullLog() {
            assertThat(ticketMapper.toLogResponse(null)).isNull();
        }
    }
}
