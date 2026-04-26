package br.com.luizbrand.worklog.tickets;

import br.com.luizbrand.worklog.auth.AuthFilter;
import br.com.luizbrand.worklog.auth.CustomUserDetailsService;
import br.com.luizbrand.worklog.client.dto.ClientSummary;
import br.com.luizbrand.worklog.client.enums.StatusFiltro;
import br.com.luizbrand.worklog.exception.NotFound.TicketNotFoundException;
import br.com.luizbrand.worklog.support.UserTestBuilder;
import br.com.luizbrand.worklog.system.dto.SystemResponse;
import br.com.luizbrand.worklog.tickets.dto.TicketFiltersParams;
import br.com.luizbrand.worklog.tickets.dto.TicketLogResponse;
import br.com.luizbrand.worklog.tickets.dto.TicketRequest;
import br.com.luizbrand.worklog.tickets.dto.TicketResponse;
import br.com.luizbrand.worklog.tickets.dto.TicketSummary;
import br.com.luizbrand.worklog.tickets.dto.TicketUpdateRequest;
import br.com.luizbrand.worklog.tickets.enums.FieldType;
import br.com.luizbrand.worklog.tickets.enums.TicketStatus;
import br.com.luizbrand.worklog.user.User;
import br.com.luizbrand.worklog.user.dto.UserSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(TicketController.class)
@AutoConfigureMockMvc(addFilters = false)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private TicketLogManager ticketLogManager;

    @MockitoBean
    private AuthFilter authFilter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private UUID ticketPublicId;
    private UUID clientPublicId;
    private UUID systemPublicId;
    private UUID userPublicId;
    private TicketResponse ticketResponse;
    private User authenticatedUser;

    @BeforeEach
    void setUp() {
        ticketPublicId = UUID.fromString("0abcfc81-9411-40a6-8cbc-d3f690da4ef0");
        clientPublicId = UUID.fromString("0abcfc81-9411-40a6-8cbc-d3f690da4ef1");
        systemPublicId = UUID.fromString("0abcfc81-9411-40a6-8cbc-d3f690da4ef2");
        userPublicId = UUID.fromString("0abcfc81-9411-40a6-8cbc-d3f690da4ef3");
        authenticatedUser = UserTestBuilder.aUser()
                .withEmail("editor@worklog.test")
                .build();

        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 21, 10, 0);
        ticketResponse = TicketResponse.builder()
                .publicId(ticketPublicId)
                .title("Ticket X")
                .description("Desc")
                .solution(null)
                .status(TicketStatus.PENDING)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .completedAt(null)
                .client(new ClientSummary(clientPublicId, "Acme", true))
                .system(new SystemResponse(systemPublicId, "Billing"))
                .user(new UserSummary(userPublicId, "Assignee", "assignee@worklog.test"))
                .build();
    }

    @Nested
    @DisplayName("Endpoint: GET /tickets/{publicId}")
    class FindTicketByPublicId {

        @Test
        @DisplayName("Should return 200 OK with the ticket when it exists")
        void shouldReturnTicketWhenFound() throws Exception {
            when(ticketService.findTicketByPublicId(ticketPublicId)).thenReturn(ticketResponse);

            mockMvc.perform(get("/tickets/{publicId}", ticketPublicId).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.publicId").value(ticketPublicId.toString()))
                    .andExpect(jsonPath("$.title").value("Ticket X"))
                    .andExpect(jsonPath("$.status").value(TicketStatus.PENDING.name()))
                    .andExpect(jsonPath("$.client.publicId").value(clientPublicId.toString()))
                    .andExpect(jsonPath("$.system.publicId").value(systemPublicId.toString()))
                    .andExpect(jsonPath("$.user.publicId").value(userPublicId.toString()));
        }

        @Test
        @DisplayName("Should return 404 Not Found when the ticket does not exist")
        void shouldReturn404WhenNotFound() throws Exception {
            String message = "Ticket not found with publicId: " + ticketPublicId;
            when(ticketService.findTicketByPublicId(ticketPublicId))
                    .thenThrow(new TicketNotFoundException(message));

            mockMvc.perform(get("/tickets/{publicId}", ticketPublicId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(message));
        }
    }

    @Nested
    @DisplayName("Endpoint: POST /tickets/create")
    class CreateTicket {

        @Test
        @DisplayName("Should return 201 Created with the new ticket on a valid request")
        void shouldReturn201OnSuccess() throws Exception {
            TicketRequest request = new TicketRequest(
                    "Ticket X", "Desc", null,
                    TicketStatus.PENDING, null,
                    clientPublicId, systemPublicId, userPublicId);
            when(ticketService.createTicket(any(TicketRequest.class))).thenReturn(ticketResponse);

            mockMvc.perform(post("/tickets/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.publicId").value(ticketPublicId.toString()))
                    .andExpect(jsonPath("$.title").value("Ticket X"))
                    .andExpect(jsonPath("$.status").value(TicketStatus.PENDING.name()));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when the payload fails validation")
        void shouldReturn400OnValidationFailure() throws Exception {
            TicketRequest invalid = new TicketRequest(
                    "", "", null,
                    null, null,
                    null, null, null);

            mockMvc.perform(post("/tickets/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }

    @Nested
    @DisplayName("Endpoint: PUT /tickets/update/{ticketPublicId}")
    class UpdateTicket {

        @BeforeEach
        void authenticate() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            authenticatedUser, null, authenticatedUser.getAuthorities()));
        }

        @AfterEach
        void clearAuthentication() {
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Should return 200 OK with the updated ticket on a valid request")
        void shouldReturn200OnSuccess() throws Exception {
            TicketUpdateRequest request = new TicketUpdateRequest(
                    "Updated title", null, "Solved",
                    TicketStatus.COMPLETED, LocalDateTime.of(2026, 4, 21, 12, 0));
            TicketResponse updated = TicketResponse.builder()
                    .publicId(ticketPublicId)
                    .title("Updated title")
                    .description(ticketResponse.description())
                    .solution("Solved")
                    .status(TicketStatus.COMPLETED)
                    .createdAt(ticketResponse.createdAt())
                    .updatedAt(ticketResponse.updatedAt())
                    .completedAt(LocalDateTime.of(2026, 4, 21, 12, 0))
                    .client(ticketResponse.client())
                    .system(ticketResponse.system())
                    .user(ticketResponse.user())
                    .build();
            when(ticketService.updateTicket(eq(ticketPublicId), any(TicketUpdateRequest.class), any(User.class)))
                    .thenReturn(updated);

            mockMvc.perform(put("/tickets/update/{ticketPublicId}", ticketPublicId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated title"))
                    .andExpect(jsonPath("$.solution").value("Solved"))
                    .andExpect(jsonPath("$.status").value(TicketStatus.COMPLETED.name()));
        }

        @Test
        @DisplayName("Should return 404 Not Found when updating a non-existent ticket")
        void shouldReturn404WhenUpdatingMissingTicket() throws Exception {
            TicketUpdateRequest request = new TicketUpdateRequest(
                    "any", null, null, null, null);
            String message = "Ticket not found with publicId: " + ticketPublicId;
            when(ticketService.updateTicket(eq(ticketPublicId), any(TicketUpdateRequest.class), any(User.class)))
                    .thenThrow(new TicketNotFoundException(message));

            mockMvc.perform(put("/tickets/update/{ticketPublicId}", ticketPublicId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(message));
        }

        @Test
        @DisplayName("Should pass the authenticated principal through to the service")
        void shouldPassAuthenticatedPrincipalThroughToService() throws Exception {
            TicketUpdateRequest request = new TicketUpdateRequest(
                    "Updated title", null, null, null, null);
            when(ticketService.updateTicket(eq(ticketPublicId), any(TicketUpdateRequest.class), eq(authenticatedUser)))
                    .thenReturn(ticketResponse);

            mockMvc.perform(put("/tickets/update/{ticketPublicId}", ticketPublicId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(ticketService, times(1))
                    .updateTicket(eq(ticketPublicId), any(TicketUpdateRequest.class), eq(authenticatedUser));
        }
    }

    @Nested
    @DisplayName("Endpoint: GET /tickets")
    class FindAllTickets {

        @BeforeEach
        void authenticate() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            authenticatedUser, null, authenticatedUser.getAuthorities()));
        }

        @AfterEach
        void clearAuthentication() {
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Should return 200 OK with a page of summaries and pass filters and paging to the service")
        void shouldReturnPagedSummaries() throws Exception {
            TicketSummary summary = TicketSummary.builder()
                    .publicId(ticketPublicId)
                    .title("Ticket X")
                    .description("Desc")
                    .status(TicketStatus.PENDING)
                    .createdAt(LocalDateTime.of(2026, 4, 21, 10, 0))
                    .updatedAt(LocalDateTime.of(2026, 4, 21, 10, 0))
                    .completedAt(null)
                    .client(ticketResponse.client())
                    .system(ticketResponse.system())
                    .user(ticketResponse.user())
                    .build();
            Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
            Page<TicketSummary> page = new PageImpl<>(List.of(summary), pageable, 1);

            when(ticketService.findAll(any(TicketFiltersParams.class), any(Pageable.class), any(User.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/tickets")
                            .param("title", "Login")
                            .param("status", "PENDING")
                            .param("clientId", clientPublicId.toString())
                            .param("systemId", systemPublicId.toString())
                            .param("userId", userPublicId.toString())
                            .param("createdFrom", "2026-04-01")
                            .param("createdTo", "2026-04-30")
                            .param("visibility", "TODOS")
                            .param("page", "0")
                            .param("size", "20")
                            .param("sort", "createdAt,desc")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content[0].publicId").value(ticketPublicId.toString()))
                    .andExpect(jsonPath("$.content[0].title").value("Ticket X"))
                    .andExpect(jsonPath("$.content[0].status").value(TicketStatus.PENDING.name()))
                    .andExpect(jsonPath("$.content[0].solution").doesNotExist())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.number").value(0))
                    .andExpect(jsonPath("$.size").value(20));

            ArgumentCaptor<TicketFiltersParams> filtersCaptor = ArgumentCaptor.forClass(TicketFiltersParams.class);
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            ArgumentCaptor<User> principalCaptor = ArgumentCaptor.forClass(User.class);
            verify(ticketService, times(1))
                    .findAll(filtersCaptor.capture(), pageableCaptor.capture(), principalCaptor.capture());

            TicketFiltersParams filters = filtersCaptor.getValue();
            assertThat(filters.title()).isEqualTo("Login");
            assertThat(filters.status()).isEqualTo(TicketStatus.PENDING);
            assertThat(filters.clientId()).isEqualTo(clientPublicId);
            assertThat(filters.systemId()).isEqualTo(systemPublicId);
            assertThat(filters.userId()).isEqualTo(userPublicId);
            assertThat(filters.createdFrom()).isEqualTo(LocalDate.of(2026, 4, 1));
            assertThat(filters.createdTo()).isEqualTo(LocalDate.of(2026, 4, 30));
            assertThat(filters.visibility()).isEqualTo(StatusFiltro.TODOS);

            Pageable captured = pageableCaptor.getValue();
            assertThat(captured.getPageNumber()).isEqualTo(0);
            assertThat(captured.getPageSize()).isEqualTo(20);
            assertThat(captured.getSort().getOrderFor("createdAt")).isNotNull();
            assertThat(captured.getSort().getOrderFor("createdAt").isDescending()).isTrue();

            assertThat(principalCaptor.getValue()).isEqualTo(authenticatedUser);
        }

        @Test
        @DisplayName("Should return 200 OK with an empty page when no ticket matches")
        void shouldReturnEmptyPage() throws Exception {
            when(ticketService.findAll(any(TicketFiltersParams.class), any(Pageable.class), any(User.class)))
                    .thenReturn(Page.empty(PageRequest.of(0, 20)));

            mockMvc.perform(get("/tickets").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("Endpoint: DELETE /tickets/{publicId}")
    class DeleteTicket {

        @Test
        @DisplayName("Should return 204 No Content when the ticket is soft-deleted")
        void shouldReturn204OnSuccess() throws Exception {
            mockMvc.perform(delete("/tickets/{publicId}", ticketPublicId))
                    .andExpect(status().isNoContent());

            verify(ticketService, times(1)).softDeleteTicket(ticketPublicId);
        }

        @Test
        @DisplayName("Should return 404 Not Found when the ticket does not exist")
        void shouldReturn404WhenMissing() throws Exception {
            String message = "Ticket not found with publicId: " + ticketPublicId;
            org.mockito.Mockito.doThrow(new TicketNotFoundException(message))
                    .when(ticketService).softDeleteTicket(ticketPublicId);

            mockMvc.perform(delete("/tickets/{publicId}", ticketPublicId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(message));
        }
    }

    @Nested
    @DisplayName("Endpoint: GET /tickets/{publicId}/logs")
    class GetTicketLogs {

        @Test
        @DisplayName("Should return 200 OK with a page of log responses and pass the pageable through to the manager")
        void shouldReturnPagedLogs() throws Exception {
            UUID changeGroupId = UUID.fromString("0abcfc81-9411-40a6-8cbc-d3f690daeeee");
            LocalDateTime changeDate = LocalDateTime.of(2026, 4, 21, 12, 0);
            TicketLogResponse log = TicketLogResponse.builder()
                    .changeGroupId(changeGroupId)
                    .fieldChanged("title")
                    .fieldType(FieldType.STRING)
                    .oldValue("Old").newValue("New")
                    .changeDate(changeDate)
                    .user(new UserSummary(userPublicId, "Editor", "editor@worklog.test"))
                    .build();
            Pageable pageable = PageRequest.of(0, 20);
            Page<TicketLogResponse> page = new PageImpl<>(List.of(log), pageable, 1);

            when(ticketLogManager.findLogsByTicket(eq(ticketPublicId), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/tickets/{publicId}/logs", ticketPublicId)
                            .param("page", "0")
                            .param("size", "20")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content[0].changeGroupId").value(changeGroupId.toString()))
                    .andExpect(jsonPath("$.content[0].fieldChanged").value("title"))
                    .andExpect(jsonPath("$.content[0].fieldType").value(FieldType.STRING.name()))
                    .andExpect(jsonPath("$.content[0].oldValue").value("Old"))
                    .andExpect(jsonPath("$.content[0].newValue").value("New"))
                    .andExpect(jsonPath("$.content[0].user.publicId").value(userPublicId.toString()))
                    .andExpect(jsonPath("$.totalElements").value(1));

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(ticketLogManager, times(1)).findLogsByTicket(eq(ticketPublicId), captor.capture());
            assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
            assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        }

        @Test
        @DisplayName("Should return 404 Not Found when the ticket does not exist")
        void shouldReturn404WhenTicketMissing() throws Exception {
            String message = "Ticket not found with publicId: " + ticketPublicId;
            when(ticketLogManager.findLogsByTicket(eq(ticketPublicId), any(Pageable.class)))
                    .thenThrow(new br.com.luizbrand.worklog.exception.NotFound.TicketNotFoundException(message));

            mockMvc.perform(get("/tickets/{publicId}/logs", ticketPublicId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(message));
        }
    }
}
