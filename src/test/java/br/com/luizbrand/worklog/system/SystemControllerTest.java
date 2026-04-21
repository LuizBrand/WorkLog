package br.com.luizbrand.worklog.system;

import br.com.luizbrand.worklog.auth.AuthFilter;
import br.com.luizbrand.worklog.auth.CustomUserDetailsService;
import br.com.luizbrand.worklog.exception.Conflict.SystemAlreadyExistsException;
import br.com.luizbrand.worklog.exception.NotFound.SystemNotFoundException;
import br.com.luizbrand.worklog.system.dto.SystemRequest;
import br.com.luizbrand.worklog.system.dto.SystemResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(SystemController.class)
@AutoConfigureMockMvc(addFilters = false)
class SystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SystemService systemService;

    @MockitoBean
    private AuthFilter authFilter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID systemPublicId;
    private SystemResponse systemResponse;

    @BeforeEach
    void setUp() {
        systemPublicId = UUID.fromString("0abcfc81-9411-40a6-8cbc-d3f690da4ef0");
        systemResponse = new SystemResponse(systemPublicId, "Billing");
    }

    @Nested
    @DisplayName("Endpoint: GET /systems")
    class FindAllSystems {

        @Test
        @DisplayName("Should return 200 OK with the list of systems")
        void shouldReturnSystemsList() throws Exception {
            SystemResponse other = new SystemResponse(UUID.randomUUID(), "CRM");
            when(systemService.findAllSystems()).thenReturn(List.of(systemResponse, other));

            mockMvc.perform(get("/systems").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].publicId").value(systemPublicId.toString()))
                    .andExpect(jsonPath("$[0].name").value("Billing"))
                    .andExpect(jsonPath("$[1].name").value("CRM"));
        }
    }

    @Nested
    @DisplayName("Endpoint: GET /systems/{publicId}")
    class FindSystemByPublicId {

        @Test
        @DisplayName("Should return 200 OK with the system when it exists")
        void shouldReturnSystemWhenFound() throws Exception {
            when(systemService.getSystemByPublicId(systemPublicId)).thenReturn(systemResponse);

            mockMvc.perform(get("/systems/{publicId}", systemPublicId).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicId").value(systemPublicId.toString()))
                    .andExpect(jsonPath("$.name").value("Billing"));
        }

        @Test
        @DisplayName("Should return 404 Not Found when the system does not exist")
        void shouldReturn404WhenMissing() throws Exception {
            String message = "System with public ID: " + systemPublicId + " not found";
            when(systemService.getSystemByPublicId(systemPublicId))
                    .thenThrow(new SystemNotFoundException(message));

            mockMvc.perform(get("/systems/{publicId}", systemPublicId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(message));
        }
    }

    @Nested
    @DisplayName("Endpoint: POST /systems/")
    class CreateSystem {

        @Test
        @DisplayName("Should return 201 Created with the new system on a valid request")
        void shouldReturn201OnSuccess() throws Exception {
            SystemRequest request = new SystemRequest("Billing");
            when(systemService.createSystem(any(SystemRequest.class))).thenReturn(systemResponse);

            mockMvc.perform(post("/systems/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.publicId").value(systemPublicId.toString()))
                    .andExpect(jsonPath("$.name").value("Billing"));
        }

        @Test
        @DisplayName("Should return 409 Conflict when the name already exists")
        void shouldReturn409OnDuplicate() throws Exception {
            SystemRequest request = new SystemRequest("Billing");
            String message = "System with name: Billing already exists";
            when(systemService.createSystem(any(SystemRequest.class)))
                    .thenThrow(new SystemAlreadyExistsException(message));

            mockMvc.perform(post("/systems/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(message));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when the name is blank")
        void shouldReturn400OnBlankName() throws Exception {
            SystemRequest invalid = new SystemRequest("");

            mockMvc.perform(post("/systems/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }

    @Nested
    @DisplayName("Endpoint: PATCH /systems/{publicId}")
    class UpdateSystem {

        @Test
        @DisplayName("Should return 200 OK with the updated system on a valid request")
        void shouldReturn200OnSuccess() throws Exception {
            SystemRequest request = new SystemRequest("Billing v2");
            SystemResponse updated = new SystemResponse(systemPublicId, "Billing v2");
            when(systemService.updateSystem(any(SystemRequest.class), eq(systemPublicId))).thenReturn(updated);

            mockMvc.perform(patch("/systems/{publicId}", systemPublicId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Billing v2"));
        }

        @Test
        @DisplayName("Should return 404 Not Found when updating a non-existent system")
        void shouldReturn404WhenMissing() throws Exception {
            SystemRequest request = new SystemRequest("Billing v2");
            String message = "System with public ID: " + systemPublicId + " not found";
            when(systemService.updateSystem(any(SystemRequest.class), eq(systemPublicId)))
                    .thenThrow(new SystemNotFoundException(message));

            mockMvc.perform(patch("/systems/{publicId}", systemPublicId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(message));
        }
    }
}
