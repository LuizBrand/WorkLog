package br.com.luizbrand.worklog.client;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import br.com.luizbrand.worklog.auth.AuthFilter;
import br.com.luizbrand.worklog.auth.CustomUserDetailsService;
import br.com.luizbrand.worklog.client.dto.ClientFiltersParams;
import br.com.luizbrand.worklog.client.dto.ClientRequest;
import br.com.luizbrand.worklog.client.dto.ClientResponse;
import br.com.luizbrand.worklog.client.dto.ClientUpdateRequest;
import br.com.luizbrand.worklog.client.enums.ClientType;
import br.com.luizbrand.worklog.client.enums.StatusFiltro;
import br.com.luizbrand.worklog.exception.Conflict.ClientAlreadyExistsException;
import br.com.luizbrand.worklog.exception.NotFound.ClientNotFoundException;
import br.com.luizbrand.worklog.support.ClientRequestTestBuilder;
import br.com.luizbrand.worklog.system.dto.SystemResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.aClientRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(ClientController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClientService clientService;

    @MockitoBean
    private AuthFilter authFilter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private ClientResponse clientResponse;
    private UUID nonExistenId;
    private String notFoundExpectedMessage;
    private UUID publicId;

    @BeforeEach
    void setUp() {

        LocalDateTime createdDate = LocalDateTime.of(2025, 9, 27, 10, 30, 00);
        OffsetDateTime mockDate = OffsetDateTime.of(createdDate, ZoneOffset.UTC);
        nonExistenId = UUID.randomUUID();
        publicId = UUID.fromString("0abcfc81-9411-40a6-8cbc-d3f690da4ef0");
        UUID systemId = UUID.fromString("0abcfc81-9411-40a6-8cbc-d3f690da4ef1");
        notFoundExpectedMessage =  "Client with public ID: " + nonExistenId + " not found";
        SystemResponse systemResponse = new SystemResponse(systemId, "System Name", true);

        clientResponse = new ClientResponse(
                publicId,
                ClientType.PJ,
                "Client Name",
                null,
                null,
                true,
                mockDate,
                List.of(systemResponse),
                List.of()
        );

    }

    private static ClientResponse withName(ClientResponse source, String name) {
        return new ClientResponse(source.publicId(), source.tipo(), name, source.nomeFantasia(),
                source.regimeTributario(), source.enabled(), source.createdAt(),
                source.systems(), source.branches());
    }

    private static ClientResponse disabled(ClientResponse source) {
        return new ClientResponse(source.publicId(), source.tipo(), source.name(), source.nomeFantasia(),
                source.regimeTributario(), false, source.createdAt(),
                source.systems(), source.branches());
    }


    @Nested
    @DisplayName("Endpoint: GET /clients/{publicId}")
    class findClientByPublicId {

        @Test
        @DisplayName("Should return 200 OK status and an Client when ID exists")
        void shouldReturnAClientWhenFoundByPublicId() throws Exception {

            when(clientService.getClientByPublicId(clientResponse.publicId()))
                    .thenReturn(clientResponse);

            mockMvc.perform(get("/clients/{publicId}", clientResponse.publicId()).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.publicId").value(clientResponse.publicId().toString()))
                    .andExpect(jsonPath("$.name").value(clientResponse.name()))
                    .andExpect(jsonPath("$.enabled").value(clientResponse.enabled()))
                    .andExpect(jsonPath("$.systems[0].publicId").value(clientResponse.systems().get(0).publicId().toString()))
                    .andExpect(jsonPath("$.systems[0].name").value(clientResponse.systems().get(0).name()));
        }

        @Test
        @DisplayName("Should return 404 not found when Client does not exists")
        void shouldReturnNotFoundWhenClientDoesNotExists() throws Exception {

            when(clientService.getClientByPublicId(nonExistenId))
                    .thenThrow(new ClientNotFoundException(notFoundExpectedMessage));

            mockMvc.perform(get("/clients/{publicId}", nonExistenId).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(notFoundExpectedMessage));

        }
    }

    @Nested
    @DisplayName("Endpoint: PATCH /clients/{publicId}")
    class updateClient {

        @Test
        @DisplayName("Should return 200 OK and an updated client")
        void shouldReturnAnUpdatedClientWhenClientExists() throws Exception {

            ClientUpdateRequest clientRequest = new ClientUpdateRequest(
                    null, "Updated Client Name", null, null, List.of(publicId), null);
            ClientResponse clientUpdated = withName(clientResponse, clientRequest.name());

            when(clientService.updateClient(clientResponse.publicId(), clientRequest)).thenReturn(clientUpdated);

            mockMvc.perform(patch("/clients/{publicId}", clientResponse.publicId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(clientRequest)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.publicId").value(clientUpdated.publicId().toString()))
                    .andExpect(jsonPath("$.name").value(clientUpdated.name()))
                    .andExpect(jsonPath("$.enabled").value(clientUpdated.enabled()))
                    .andExpect(jsonPath("$.systems[0].publicId").value(clientResponse.systems().get(0).publicId().toString()))
                    .andExpect(jsonPath("$.systems[0].name").value(clientResponse.systems().get(0).name()));
        }

        @Test
        @DisplayName("Should return 404 Not Found when updating a non-existent client")
        void shouldReturn404WhenUpdatingMissingClient() throws Exception {
            ClientUpdateRequest clientRequest = new ClientUpdateRequest(
                    null, "Updated Client Name", null, null, List.of(publicId), null);
            when(clientService.updateClient(nonExistenId, clientRequest))
                    .thenThrow(new ClientNotFoundException(notFoundExpectedMessage));

            mockMvc.perform(patch("/clients/{publicId}", nonExistenId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(clientRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(notFoundExpectedMessage));
        }

        @Test
        @DisplayName("Should return 200 OK with enabled=false when PATCH carries enabled=false")
        void shouldReturnDisabledWhenPatchSetsEnabledFalse() throws Exception {
            UUID systemId = clientResponse.systems().get(0).publicId();
            ClientUpdateRequest clientRequest = new ClientUpdateRequest(
                    null, "Client Name", null, null, List.of(systemId), false);
            ClientResponse clientUpdated = disabled(clientResponse);

            when(clientService.updateClient(clientResponse.publicId(), clientRequest)).thenReturn(clientUpdated);

            mockMvc.perform(patch("/clients/{publicId}", clientResponse.publicId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(clientRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(false));
        }
    }

    @Nested
    @DisplayName("Endpoint: DELETE /clients/{publicId}")
    class DeleteClient {

        @Test
        @DisplayName("Should return 204 No Content when the client is soft-deleted")
        void shouldReturn204OnSuccess() throws Exception {
            mockMvc.perform(delete("/clients/{publicId}", publicId))
                    .andExpect(status().isNoContent());

            org.mockito.Mockito.verify(clientService, org.mockito.Mockito.times(1)).softDeleteClient(publicId);
        }

        @Test
        @DisplayName("Should return 404 Not Found when the client does not exist")
        void shouldReturn404WhenMissing() throws Exception {
            org.mockito.Mockito.doThrow(new ClientNotFoundException(notFoundExpectedMessage))
                    .when(clientService).softDeleteClient(nonExistenId);

            mockMvc.perform(delete("/clients/{publicId}", nonExistenId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(notFoundExpectedMessage));
        }
    }

    @Nested
    @DisplayName("Endpoint: GET /clients")
    class findAllClients {

        @Test
        @DisplayName("Should return 200 OK with the list of clients and pass filters through")
        void shouldReturnFilteredClients() throws Exception {
            when(clientService.findAllClients(any(ClientFiltersParams.class)))
                    .thenReturn(List.of(clientResponse));

            mockMvc.perform(get("/clients")
                            .param("name", "Client")
                            .param("status", StatusFiltro.ATIVO.name())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].publicId").value(clientResponse.publicId().toString()))
                    .andExpect(jsonPath("$[0].name").value(clientResponse.name()));

            verify(clientService, never()).findAllClients(any(ClientFiltersParams.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return a Page when page is present in the query string")
        void shouldReturnPageWhenPageParamIsPresent() throws Exception {
            when(clientService.findAllClients(any(ClientFiltersParams.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(clientResponse), PageRequest.of(1, 12), 26));

            mockMvc.perform(get("/clients")
                            .param("page", "1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].publicId").value(clientResponse.publicId().toString()))
                    .andExpect(jsonPath("$.totalElements").value(26))
                    .andExpect(jsonPath("$.totalPages").value(3))
                    .andExpect(jsonPath("$.number").value(1))
                    .andExpect(jsonPath("$.size").value(12));

            verify(clientService, never()).findAllClients(any(ClientFiltersParams.class));
        }

        @Test
        @DisplayName("Should return a Page when only size is present in the query string")
        void shouldReturnPageWhenOnlySizeParamIsPresent() throws Exception {
            when(clientService.findAllClients(any(ClientFiltersParams.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(clientResponse), PageRequest.of(0, 5), 1));

            mockMvc.perform(get("/clients")
                            .param("size", "5")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(clientService).findAllClients(any(ClientFiltersParams.class), pageableCaptor.capture());
            assertEquals(5, pageableCaptor.getValue().getPageSize());
        }

        @Test
        @DisplayName("Should default to 12 per page sorted by name ascending")
        void shouldApplyDefaultPageSizeAndSort() throws Exception {
            when(clientService.findAllClients(any(ClientFiltersParams.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(clientResponse), PageRequest.of(0, 12), 1));

            mockMvc.perform(get("/clients")
                            .param("page", "0")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(clientService).findAllClients(any(ClientFiltersParams.class), pageableCaptor.capture());

            Pageable pageable = pageableCaptor.getValue();
            assertEquals(0, pageable.getPageNumber());
            assertEquals(12, pageable.getPageSize());
            assertEquals(Sort.by(Sort.Direction.ASC, "name"), pageable.getSort());
        }

        @Test
        @DisplayName("Should keep the filters when paginating")
        void shouldPassFiltersWhenPaginating() throws Exception {
            when(clientService.findAllClients(any(ClientFiltersParams.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(clientResponse), PageRequest.of(0, 12), 1));

            mockMvc.perform(get("/clients")
                            .param("name", "Client")
                            .param("status", StatusFiltro.ATIVO.name())
                            .param("tipo", ClientType.PJ.name())
                            .param("page", "0")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            ArgumentCaptor<ClientFiltersParams> filtersCaptor = ArgumentCaptor.forClass(ClientFiltersParams.class);
            verify(clientService).findAllClients(filtersCaptor.capture(), any(Pageable.class));

            ClientFiltersParams filters = filtersCaptor.getValue();
            assertEquals("Client", filters.name());
            assertEquals(StatusFiltro.ATIVO, filters.status());
            assertEquals(ClientType.PJ, filters.tipo());
        }
    }

    @Nested
    @DisplayName("Endpoint: POST /clients/")
    class createClient {

        @Test
        @DisplayName("Should return 201 Created with the new client on a valid request")
        void shouldReturn201OnSuccess() throws Exception {
            UUID systemId = clientResponse.systems().get(0).publicId();
            ClientRequest request = aClientRequest().withName("Client Name").withSystem(systemId).build();

            when(clientService.createClient(any(ClientRequest.class))).thenReturn(clientResponse);

            mockMvc.perform(post("/clients/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.publicId").value(clientResponse.publicId().toString()))
                    .andExpect(jsonPath("$.name").value(clientResponse.name()));
        }

        @Test
        @DisplayName("Should return 409 Conflict when the name already exists")
        void shouldReturn409OnDuplicate() throws Exception {
            UUID systemId = clientResponse.systems().get(0).publicId();
            ClientRequest request = aClientRequest().withName("Client Name").withSystem(systemId).build();
            String message = "Client with name: Client Name already exists";
            when(clientService.createClient(any(ClientRequest.class)))
                    .thenThrow(new ClientAlreadyExistsException(message));

            mockMvc.perform(post("/clients/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(message));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when the name is blank")
        void shouldReturn400OnBlankName() throws Exception {
            UUID systemId = clientResponse.systems().get(0).publicId();
            ClientRequest invalid = aClientRequest().withName("").withSystem(systemId).build();

            mockMvc.perform(post("/clients/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Should create the client when systemsPublicIds is empty — associating a system is optional")
        void shouldReturn201OnEmptySystems() throws Exception {
            ClientRequest request = aClientRequest().withName("Client Name").withSystems(List.of()).build();
            when(clientService.createClient(any(ClientRequest.class))).thenReturn(clientResponse);

            mockMvc.perform(post("/clients/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should create the client when systemsPublicIds is absent")
        void shouldReturn201OnNullSystems() throws Exception {
            ClientRequest request = aClientRequest().withName("Client Name").withSystems((List<UUID>) null).build();
            when(clientService.createClient(any(ClientRequest.class))).thenReturn(clientResponse);

            mockMvc.perform(post("/clients/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when tipo is missing")
        void shouldReturn400OnMissingTipo() throws Exception {
            UUID systemId = clientResponse.systems().get(0).publicId();
            ClientRequest invalid = aClientRequest().withTipo(null).withSystem(systemId).build();

            mockMvc.perform(post("/clients/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when no branch is informed")
        void shouldReturn400OnEmptyBranches() throws Exception {
            UUID systemId = clientResponse.systems().get(0).publicId();
            ClientRequest invalid = aClientRequest().withSystem(systemId).withBranches(List.of()).build();

            mockMvc.perform(post("/clients/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when the branch document does not match the client type")
        void shouldReturn400OnInvalidDocumento() throws Exception {
            UUID systemId = clientResponse.systems().get(0).publicId();
            ClientRequest invalid = aClientRequest()
                    .withTipo(ClientType.PJ)
                    .withSystem(systemId)
                    .withDocumento(ClientRequestTestBuilder.CPF_VALIDO)
                    .build();

            mockMvc.perform(post("/clients/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }

}