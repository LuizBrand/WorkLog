package br.com.luizbrand.worklog.tickets;

import br.com.luizbrand.worklog.client.Client;
import br.com.luizbrand.worklog.client.ClientService;
import br.com.luizbrand.worklog.exception.Business.BusinessException;
import br.com.luizbrand.worklog.exception.NotFound.TicketNotFoundException;
import br.com.luizbrand.worklog.support.ClientTestBuilder;
import br.com.luizbrand.worklog.support.SystemTestBuilder;
import br.com.luizbrand.worklog.support.TicketTestBuilder;
import br.com.luizbrand.worklog.support.UserTestBuilder;
import br.com.luizbrand.worklog.system.SystemService;
import br.com.luizbrand.worklog.system.Systems;
import br.com.luizbrand.worklog.tickets.dto.TicketRequest;
import br.com.luizbrand.worklog.tickets.dto.TicketResponse;
import br.com.luizbrand.worklog.tickets.dto.TicketUpdateRequest;
import br.com.luizbrand.worklog.tickets.enums.TicketStatus;
import br.com.luizbrand.worklog.user.User;
import br.com.luizbrand.worklog.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private ClientService clientService;

    @Mock
    private SystemService systemService;

    @Mock
    private UserService userService;

    @Mock
    private TicketLogManager ticketLogManager;

    @InjectMocks
    private TicketService ticketService;

    private Client client;
    private Systems system;
    private User user;
    private Ticket ticket;
    private TicketResponse ticketResponse;

    @BeforeEach
    void setUp() {
        client = ClientTestBuilder.aClient().build();
        system = SystemTestBuilder.aSystem().build();
        user = UserTestBuilder.aUser().build();
        ticket = TicketTestBuilder.aTicket()
                .withClient(client).withSystem(system).withUser(user).build();
        ticketResponse = TicketResponse.builder()
                .publicId(ticket.getPublicId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .build();
    }

    @Nested
    @DisplayName("Method: createTicket()")
    class CreateTicketTests {

        @Test
        @DisplayName("Should resolve related entities, persist the ticket, and return the response")
        void shouldCreateTicketSuccessfully() {
            TicketRequest request = new TicketRequest(
                    "Title", "Description", null,
                    TicketStatus.PENDING, null,
                    client.getPublicId(), system.getPublicId(), user.getPublicId());

            Ticket mapped = TicketTestBuilder.aTicket()
                    .withTitle(request.title())
                    .withDescription(request.description())
                    .withStatus(request.status())
                    .withClient(null).withSystem(null).withUser(null)
                    .build();

            when(ticketMapper.toEntity(request)).thenReturn(mapped);
            when(clientService.findActiveClient(request.clientId())).thenReturn(client);
            when(systemService.findActiveSystem(request.systemId())).thenReturn(system);
            when(userService.findActiveUser(request.userId())).thenReturn(user);
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
            when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(ticketResponse);

            TicketResponse response = ticketService.createTicket(request);

            assertThat(response).isEqualTo(ticketResponse);

            ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
            verify(ticketRepository).save(captor.capture());
            Ticket persisted = captor.getValue();
            assertThat(persisted.getClient()).isEqualTo(client);
            assertThat(persisted.getSystem()).isEqualTo(system);
            assertThat(persisted.getUser()).isEqualTo(user);
        }

        @Test
        @DisplayName("Should not look up the user when userId is null")
        void shouldSkipUserLookupWhenUserIdIsNull() {
            TicketRequest request = new TicketRequest(
                    "Title", "Description", null,
                    TicketStatus.PENDING, null,
                    client.getPublicId(), system.getPublicId(), null);

            Ticket mapped = TicketTestBuilder.aTicket()
                    .withClient(null).withSystem(null).withUser(null).build();

            when(ticketMapper.toEntity(request)).thenReturn(mapped);
            when(clientService.findActiveClient(request.clientId())).thenReturn(client);
            when(systemService.findActiveSystem(request.systemId())).thenReturn(system);
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
            when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(ticketResponse);

            ticketService.createTicket(request);

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Should propagate BusinessException when the client is inactive")
        void shouldPropagateWhenClientInactive() {
            TicketRequest request = new TicketRequest(
                    "Title", "Description", null,
                    TicketStatus.PENDING, null,
                    client.getPublicId(), system.getPublicId(), null);

            when(ticketMapper.toEntity(request)).thenReturn(ticket);
            when(clientService.findActiveClient(request.clientId()))
                    .thenThrow(new BusinessException("Client is not active"));

            assertThrows(BusinessException.class, () -> ticketService.createTicket(request));

            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should propagate BusinessException when the system is inactive")
        void shouldPropagateWhenSystemInactive() {
            TicketRequest request = new TicketRequest(
                    "Title", "Description", null,
                    TicketStatus.PENDING, null,
                    client.getPublicId(), system.getPublicId(), null);

            when(ticketMapper.toEntity(request)).thenReturn(ticket);
            when(clientService.findActiveClient(request.clientId())).thenReturn(client);
            when(systemService.findActiveSystem(request.systemId()))
                    .thenThrow(new BusinessException("System is not active"));

            assertThrows(BusinessException.class, () -> ticketService.createTicket(request));

            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should propagate BusinessException when the referenced user is inactive")
        void shouldPropagateWhenUserInactive() {
            TicketRequest request = new TicketRequest(
                    "Title", "Description", null,
                    TicketStatus.PENDING, null,
                    client.getPublicId(), system.getPublicId(), user.getPublicId());

            when(ticketMapper.toEntity(request)).thenReturn(ticket);
            when(clientService.findActiveClient(request.clientId())).thenReturn(client);
            when(systemService.findActiveSystem(request.systemId())).thenReturn(system);
            when(userService.findActiveUser(request.userId()))
                    .thenThrow(new BusinessException("User is not active"));

            assertThrows(BusinessException.class, () -> ticketService.createTicket(request));

            verify(ticketRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Method: findTicketByPublicId()")
    class FindTicketByPublicIdTests {

        @Test
        @DisplayName("Should return the ticket response when it exists")
        void shouldReturnTicketWhenFound() {
            when(ticketRepository.findByPublicId(ticket.getPublicId())).thenReturn(Optional.of(ticket));
            when(ticketMapper.toResponse(ticket)).thenReturn(ticketResponse);

            TicketResponse response = ticketService.findTicketByPublicId(ticket.getPublicId());

            assertThat(response).isEqualTo(ticketResponse);
        }

        @Test
        @DisplayName("Should throw TicketNotFoundException when no ticket matches the publicId")
        void shouldThrowWhenNotFound() {
            UUID missing = UUID.randomUUID();
            when(ticketRepository.findByPublicId(missing)).thenReturn(Optional.empty());

            TicketNotFoundException ex = assertThrows(TicketNotFoundException.class,
                    () -> ticketService.findTicketByPublicId(missing));

            assertThat(ex.getMessage()).contains(missing.toString());
        }
    }

    @Nested
    @DisplayName("Method: updateTicket()")
    class UpdateTicketTests {

        @Test
        @DisplayName("Should throw TicketNotFoundException when the ticket does not exist")
        void shouldThrowWhenTicketMissing() {
            UUID missing = UUID.randomUUID();
            TicketUpdateRequest request = new TicketUpdateRequest(
                    "New title", null, null, null, null, null);

            when(ticketRepository.findByPublicId(missing)).thenReturn(Optional.empty());

            assertThrows(TicketNotFoundException.class,
                    () -> ticketService.updateTicket(missing, request));

            verify(ticketRepository, never()).save(any());
            verifyNoInteractions(ticketLogManager);
        }

        @Test
        @DisplayName("Should apply only non-null fields and delegate log generation with the original ticket owner")
        void shouldApplyPartialUpdateAndDelegateLogs() {
            LocalDateTime completion = LocalDateTime.of(2026, 4, 21, 12, 0);
            Ticket existing = TicketTestBuilder.aTicket()
                    .withPublicId(ticket.getPublicId())
                    .withTitle("Old title")
                    .withDescription("Old description")
                    .withSolution(null)
                    .withStatus(TicketStatus.PENDING)
                    .withCompletedAt(null)
                    .withClient(client).withSystem(system).withUser(user)
                    .build();

            TicketUpdateRequest request = new TicketUpdateRequest(
                    "New title", null, "Fix applied",
                    TicketStatus.COMPLETED, completion, null);

            when(ticketRepository.findByPublicId(existing.getPublicId())).thenReturn(Optional.of(existing));
            when(ticketRepository.save(existing)).thenReturn(existing);
            when(ticketMapper.toResponse(existing)).thenReturn(ticketResponse);

            TicketResponse response = ticketService.updateTicket(existing.getPublicId(), request);

            assertThat(response).isEqualTo(ticketResponse);
            // PATCH semantics: null description left unchanged
            assertThat(existing.getTitle()).isEqualTo("New title");
            assertThat(existing.getDescription()).isEqualTo("Old description");
            assertThat(existing.getSolution()).isEqualTo("Fix applied");
            assertThat(existing.getStatus()).isEqualTo(TicketStatus.COMPLETED);
            assertThat(existing.getCompletedAt()).isEqualTo(completion);

            ArgumentCaptor<Ticket> newTicketCaptor = ArgumentCaptor.forClass(Ticket.class);
            verify(ticketLogManager, times(1))
                    .generateLogs(org.mockito.ArgumentMatchers.eq(existing),
                            newTicketCaptor.capture(),
                            org.mockito.ArgumentMatchers.eq(user));
            Ticket proposed = newTicketCaptor.getValue();
            assertThat(proposed.getTitle()).isEqualTo("New title");
            assertThat(proposed.getDescription()).isEqualTo("Old description");
            assertThat(proposed.getSolution()).isEqualTo("Fix applied");
            assertThat(proposed.getStatus()).isEqualTo(TicketStatus.COMPLETED);
            assertThat(proposed.getCompletedAt()).isEqualTo(completion);

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Should resolve the new user when userId is provided in the update request")
        void shouldResolveUserWhenUserIdProvided() {
            Ticket existing = TicketTestBuilder.aTicket()
                    .withClient(client).withSystem(system).withUser(user).build();
            User newUser = UserTestBuilder.aUser()
                    .withPublicId(UUID.randomUUID())
                    .withEmail("new-owner@worklog.test").build();
            TicketUpdateRequest request = new TicketUpdateRequest(
                    null, null, null, null, null, newUser.getPublicId());

            when(ticketRepository.findByPublicId(existing.getPublicId())).thenReturn(Optional.of(existing));
            when(userService.findEntityByPublicId(newUser.getPublicId())).thenReturn(newUser);
            when(ticketRepository.save(existing)).thenReturn(existing);
            when(ticketMapper.toResponse(existing)).thenReturn(ticketResponse);

            ticketService.updateTicket(existing.getPublicId(), request);

            verify(ticketLogManager, times(1))
                    .generateLogs(org.mockito.ArgumentMatchers.eq(existing),
                            any(Ticket.class),
                            org.mockito.ArgumentMatchers.eq(newUser));
        }
    }
}
