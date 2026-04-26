package br.com.luizbrand.worklog.tickets;

import br.com.luizbrand.worklog.client.Client;
import br.com.luizbrand.worklog.client.ClientService;
import br.com.luizbrand.worklog.client.enums.StatusFiltro;
import br.com.luizbrand.worklog.exception.Business.BusinessException;
import br.com.luizbrand.worklog.exception.NotFound.TicketNotFoundException;
import br.com.luizbrand.worklog.support.ClientTestBuilder;
import br.com.luizbrand.worklog.support.RoleTestBuilder;
import br.com.luizbrand.worklog.support.SystemTestBuilder;
import br.com.luizbrand.worklog.support.TicketTestBuilder;
import br.com.luizbrand.worklog.support.UserTestBuilder;
import br.com.luizbrand.worklog.system.SystemService;
import br.com.luizbrand.worklog.system.Systems;
import br.com.luizbrand.worklog.tickets.dto.TicketFiltersParams;
import br.com.luizbrand.worklog.tickets.dto.TicketRequest;
import br.com.luizbrand.worklog.tickets.dto.TicketResponse;
import br.com.luizbrand.worklog.tickets.dto.TicketSummary;
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
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
                    "New title", null, null, null, null);

            when(ticketRepository.findByPublicId(missing)).thenReturn(Optional.empty());

            assertThrows(TicketNotFoundException.class,
                    () -> ticketService.updateTicket(missing, request, user));

            verify(ticketRepository, never()).save(any());
            verifyNoInteractions(ticketLogManager);
        }

        @Test
        @DisplayName("Should apply only non-null fields and delegate log generation using the authenticated user as author")
        void shouldApplyPartialUpdateAndUseAuthenticatedUserAsLogAuthor() {
            LocalDateTime completion = LocalDateTime.of(2026, 4, 21, 12, 0);
            User ticketOwner = UserTestBuilder.aUser()
                    .withPublicId(UUID.randomUUID())
                    .withEmail("owner@worklog.test").build();
            User editor = UserTestBuilder.aUser()
                    .withPublicId(UUID.randomUUID())
                    .withEmail("editor@worklog.test").build();

            Ticket existing = TicketTestBuilder.aTicket()
                    .withPublicId(ticket.getPublicId())
                    .withTitle("Old title")
                    .withDescription("Old description")
                    .withSolution(null)
                    .withStatus(TicketStatus.PENDING)
                    .withCompletedAt(null)
                    .withClient(client).withSystem(system).withUser(ticketOwner)
                    .build();

            TicketUpdateRequest request = new TicketUpdateRequest(
                    "New title", null, "Fix applied",
                    TicketStatus.COMPLETED, completion);

            when(ticketRepository.findByPublicId(existing.getPublicId())).thenReturn(Optional.of(existing));
            when(ticketRepository.save(existing)).thenReturn(existing);
            when(ticketMapper.toResponse(existing)).thenReturn(ticketResponse);

            TicketResponse response = ticketService.updateTicket(existing.getPublicId(), request, editor);

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
                            org.mockito.ArgumentMatchers.eq(editor));
            Ticket proposed = newTicketCaptor.getValue();
            assertThat(proposed.getTitle()).isEqualTo("New title");
            assertThat(proposed.getDescription()).isEqualTo("Old description");
            assertThat(proposed.getSolution()).isEqualTo("Fix applied");
            assertThat(proposed.getStatus()).isEqualTo(TicketStatus.COMPLETED);
            assertThat(proposed.getCompletedAt()).isEqualTo(completion);

            verifyNoInteractions(userService);
        }
    }

    @Nested
    @DisplayName("Method: findAll()")
    class FindAllTickets {

        private User adminUser() {
            return UserTestBuilder.aUser().withRole(RoleTestBuilder.adminRole()).build();
        }

        @Test
        @DisplayName("Should delegate to the repository with spec and pageable and map every entity to a summary")
        void shouldReturnMappedPage() {
            Ticket other = TicketTestBuilder.aTicket()
                    .withPublicId(UUID.randomUUID())
                    .withTitle("Other")
                    .withClient(client).withSystem(system).withUser(user)
                    .build();
            Pageable pageable = PageRequest.of(0, 10);
            TicketFiltersParams filters = new TicketFiltersParams(
                    "login", TicketStatus.PENDING,
                    client.getPublicId(), system.getPublicId(), user.getPublicId(),
                    LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), null);

            TicketSummary summaryA = TicketSummary.builder().publicId(ticket.getPublicId()).title("A").build();
            TicketSummary summaryB = TicketSummary.builder().publicId(other.getPublicId()).title("B").build();

            Page<Ticket> repoPage = new PageImpl<>(List.of(ticket, other), pageable, 2);
            when(ticketRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(repoPage);
            when(ticketMapper.toSummary(ticket)).thenReturn(summaryA);
            when(ticketMapper.toSummary(other)).thenReturn(summaryB);

            Page<TicketSummary> result = ticketService.findAll(filters, pageable, user);

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).containsExactly(summaryA, summaryB);
            verify(ticketRepository, times(1)).findAll(any(Specification.class), eq(pageable));
            verify(ticketMapper, times(1)).toSummary(ticket);
            verify(ticketMapper, times(1)).toSummary(other);
        }

        @Test
        @DisplayName("Should return an empty page when the repository has no matches")
        void shouldReturnEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            TicketFiltersParams filters = new TicketFiltersParams(null, null, null, null, null, null, null, null);
            when(ticketRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(Page.empty(pageable));

            Page<TicketSummary> result = ticketService.findAll(filters, pageable, user);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            verify(ticketMapper, never()).toSummary(any());
        }

        @Test
        @DisplayName("Should force visibility to ATIVO for a non-ADMIN principal even when the caller asked for TODOS")
        void shouldForceAtivoForNonAdminWhenCallerSentTodos() {
            assertVisibilityPassedToSpec(StatusFiltro.TODOS, user, StatusFiltro.ATIVO);
        }

        @Test
        @DisplayName("Should force visibility to ATIVO for a non-ADMIN principal even when the caller asked for INATIVO")
        void shouldForceAtivoForNonAdminWhenCallerSentInativo() {
            assertVisibilityPassedToSpec(StatusFiltro.INATIVO, user, StatusFiltro.ATIVO);
        }

        @Test
        @DisplayName("Should force visibility to ATIVO for a non-ADMIN principal when the caller did not send a visibility")
        void shouldForceAtivoForNonAdminWhenCallerSentNull() {
            assertVisibilityPassedToSpec(null, user, StatusFiltro.ATIVO);
        }

        @Test
        @DisplayName("Should preserve the caller visibility for an ADMIN principal")
        void shouldPreserveAdminCallerVisibility() {
            assertVisibilityPassedToSpec(StatusFiltro.INATIVO, adminUser(), StatusFiltro.INATIVO);
            assertVisibilityPassedToSpec(StatusFiltro.TODOS, adminUser(), StatusFiltro.TODOS);
        }

        @Test
        @DisplayName("Should default visibility to ATIVO for an ADMIN principal when no visibility was sent")
        void shouldDefaultToAtivoForAdminWhenNullVisibility() {
            assertVisibilityPassedToSpec(null, adminUser(), StatusFiltro.ATIVO);
        }

        @SuppressWarnings("unchecked")
        private void assertVisibilityPassedToSpec(StatusFiltro requested,
                                                  User principal,
                                                  StatusFiltro expected) {
            Pageable pageable = PageRequest.of(0, 10);
            TicketFiltersParams filters = new TicketFiltersParams(
                    null, null, null, null, null, null, null, requested);
            Specification<Ticket> dummy = (Specification<Ticket>) Mockito.mock(Specification.class);

            try (MockedStatic<TicketSpecification> spec = Mockito.mockStatic(TicketSpecification.class)) {
                spec.when(() -> TicketSpecification.findByFilter(any(TicketFiltersParams.class)))
                        .thenReturn(dummy);
                when(ticketRepository.findAll(any(Specification.class), eq(pageable)))
                        .thenReturn(Page.empty(pageable));

                ticketService.findAll(filters, pageable, principal);

                ArgumentCaptor<TicketFiltersParams> captor = ArgumentCaptor.forClass(TicketFiltersParams.class);
                spec.verify(() -> TicketSpecification.findByFilter(captor.capture()));
                assertThat(captor.getValue().visibility())
                        .as("effective visibility for principal %s with requested %s", principal.getEmail(), requested)
                        .isEqualTo(expected);
            }
        }
    }

    @Nested
    @DisplayName("Method: softDeleteTicket()")
    class SoftDeleteTicket {

        @Test
        @DisplayName("Should set isEnabled to false and persist the ticket")
        void shouldSoftDeleteWhenTicketExists() {
            Ticket existing = TicketTestBuilder.aTicket()
                    .withPublicId(ticket.getPublicId())
                    .withClient(client).withSystem(system).withUser(user)
                    .build();
            existing.setIsEnabled(true);

            when(ticketRepository.findByPublicId(existing.getPublicId())).thenReturn(Optional.of(existing));
            when(ticketRepository.save(existing)).thenReturn(existing);

            ticketService.softDeleteTicket(existing.getPublicId());

            assertThat(existing.getIsEnabled()).isFalse();
            verify(ticketRepository, times(1)).save(existing);
        }

        @Test
        @DisplayName("Should throw TicketNotFoundException when the ticket does not exist")
        void shouldThrowWhenMissing() {
            UUID missing = UUID.randomUUID();
            when(ticketRepository.findByPublicId(missing)).thenReturn(Optional.empty());

            TicketNotFoundException ex = assertThrows(TicketNotFoundException.class,
                    () -> ticketService.softDeleteTicket(missing));

            assertThat(ex.getMessage()).contains(missing.toString());
            verify(ticketRepository, never()).save(any());
        }
    }
}
