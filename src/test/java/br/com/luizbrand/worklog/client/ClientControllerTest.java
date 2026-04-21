package br.com.luizbrand.worklog.client;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import br.com.luizbrand.worklog.auth.AuthFilter;
import br.com.luizbrand.worklog.auth.CustomUserDetailsService;
import br.com.luizbrand.worklog.client.dto.ClientFiltersParams;
import br.com.luizbrand.worklog.client.dto.ClientRequest;
import br.com.luizbrand.worklog.client.dto.ClientResponse;
import br.com.luizbrand.worklog.client.enums.StatusFiltro;
import br.com.luizbrand.worklog.exception.Conflict.ClientAlreadyExistsException;
import br.com.luizbrand.worklog.exception.NotFound.ClientNotFoundException;
import br.com.luizbrand.worklog.system.dto.SystemResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
        SystemResponse systemResponse = new SystemResponse(systemId, "System Name");

        clientResponse = new ClientResponse(
                publicId,
                "Client Name",
                true,
                mockDate,
                List.of(systemResponse)
        );

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

            ClientRequest clientRequest = new ClientRequest("Updated Client Name", List.of(publicId));
            ClientResponse clientUpdated = new ClientResponse(
                    clientResponse.publicId(),
                    clientRequest.name(),
                    clientResponse.enabled(),
                    clientResponse.createdAt(),
                    clientResponse.systems()
            );

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
            ClientRequest clientRequest = new ClientRequest("Updated Client Name", List.of(publicId));
            when(clientService.updateClient(nonExistenId, clientRequest))
                    .thenThrow(new ClientNotFoundException(notFoundExpectedMessage));

            mockMvc.perform(patch("/clients/{publicId}", nonExistenId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(clientRequest)))
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
        }
    }

    @Nested
    @DisplayName("Endpoint: POST /clients/")
    class createClient {

        @Test
        @DisplayName("Should return 201 Created with the new client on a valid request")
        void shouldReturn201OnSuccess() throws Exception {
            UUID systemId = clientResponse.systems().get(0).publicId();
            ClientRequest request = new ClientRequest("Client Name", List.of(systemId));

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
            ClientRequest request = new ClientRequest("Client Name", List.of(systemId));
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
            ClientRequest invalid = new ClientRequest("", List.of(systemId));

            mockMvc.perform(post("/clients/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when systemsPublicIds is empty")
        void shouldReturn400OnEmptySystems() throws Exception {
            ClientRequest invalid = new ClientRequest("Client Name", List.of());

            mockMvc.perform(post("/clients/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }

}