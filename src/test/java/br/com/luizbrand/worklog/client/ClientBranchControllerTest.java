package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.auth.AuthFilter;
import br.com.luizbrand.worklog.auth.CustomUserDetailsService;
import br.com.luizbrand.worklog.client.dto.BranchRequest;
import br.com.luizbrand.worklog.client.dto.BranchResponse;
import br.com.luizbrand.worklog.client.enums.StatusFiltro;
import br.com.luizbrand.worklog.exception.Business.BusinessException;
import br.com.luizbrand.worklog.exception.Conflict.DocumentoAlreadyExistsException;
import br.com.luizbrand.worklog.exception.NotFound.ClientNotFoundException;
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

import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.CNPJ_ALFANUMERICO_VALIDO;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(ClientBranchController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClientBranchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClientBranchService clientBranchService;

    @MockitoBean
    private AuthFilter authFilter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private UUID clientPublicId;
    private BranchResponse branchResponse;

    @BeforeEach
    void setUp() {
        clientPublicId = UUID.fromString("0abcfc81-9411-40a6-8cbc-d3f690da4ef0");
        branchResponse = new BranchResponse(UUID.randomUUID(), CNPJ_ALFANUMERICO_VALIDO, false,
                "Filial SP", null, null, null, List.of(), true);
    }

    @Nested
    @DisplayName("Endpoint: GET /clients/{publicId}/branches")
    class FindBranches {

        @Test
        @DisplayName("Deve retornar 200 e a lista de filiais")
        void shouldReturn200WithBranches() throws Exception {
            when(clientBranchService.findBranchesByClient(clientPublicId, null))
                    .thenReturn(List.of(branchResponse));

            mockMvc.perform(get("/clients/{publicId}/branches", clientPublicId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                    .andExpect(jsonPath("$[0].documento").value(CNPJ_ALFANUMERICO_VALIDO))
                    .andExpect(jsonPath("$[0].isMatriz").value(false))
                    .andExpect(jsonPath("$[0].enabled").value(true));
        }

        @Test
        @DisplayName("Deve repassar o filtro de status para o serviço")
        void shouldForwardStatusFilter() throws Exception {
            when(clientBranchService.findBranchesByClient(clientPublicId, StatusFiltro.ATIVO))
                    .thenReturn(List.of());

            mockMvc.perform(get("/clients/{publicId}/branches", clientPublicId)
                            .param("status", "ATIVO")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(clientBranchService).findBranchesByClient(clientPublicId, StatusFiltro.ATIVO);
        }

        @Test
        @DisplayName("Deve retornar 404 quando o cliente não existe")
        void shouldReturn404WhenClientMissing() throws Exception {
            when(clientBranchService.findBranchesByClient(eq(clientPublicId), any()))
                    .thenThrow(new ClientNotFoundException("Client with public ID: " + clientPublicId + " not found"));

            mockMvc.perform(get("/clients/{publicId}/branches", clientPublicId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Endpoint: POST /clients/{publicId}/branches")
    class CreateBranch {

        private BranchRequest aValidRequest() {
            return new BranchRequest(CNPJ_ALFANUMERICO_VALIDO, false, "Filial SP",
                    null, null, null, List.of());
        }

        @Test
        @DisplayName("Deve retornar 201 e a filial criada")
        void shouldReturn201OnSuccess() throws Exception {
            when(clientBranchService.createBranch(eq(clientPublicId), any(BranchRequest.class)))
                    .thenReturn(branchResponse);

            mockMvc.perform(post("/clients/{publicId}/branches", clientPublicId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(aValidRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.documento").value(CNPJ_ALFANUMERICO_VALIDO))
                    .andExpect(jsonPath("$.apelido").value("Filial SP"));
        }

        @Test
        @DisplayName("Deve retornar 404 quando o cliente não existe")
        void shouldReturn404WhenClientMissing() throws Exception {
            when(clientBranchService.createBranch(eq(clientPublicId), any(BranchRequest.class)))
                    .thenThrow(new ClientNotFoundException("Client with public ID: " + clientPublicId + " not found"));

            mockMvc.perform(post("/clients/{publicId}/branches", clientPublicId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(aValidRequest())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 422 quando o serviço rejeita a regra de negócio")
        void shouldReturn422OnBusinessRuleViolation() throws Exception {
            when(clientBranchService.createBranch(eq(clientPublicId), any(BranchRequest.class)))
                    .thenThrow(new BusinessException("Cliente pessoa física deve ter apenas a matriz"));

            mockMvc.perform(post("/clients/{publicId}/branches", clientPublicId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(aValidRequest())))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("Deve retornar 409 quando o documento já existe")
        void shouldReturn409OnDuplicatedDocumento() throws Exception {
            when(clientBranchService.createBranch(eq(clientPublicId), any(BranchRequest.class)))
                    .thenThrow(new DocumentoAlreadyExistsException("Documento: " + CNPJ_ALFANUMERICO_VALIDO + " already exists"));

            mockMvc.perform(post("/clients/{publicId}/branches", clientPublicId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(aValidRequest())))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Deve retornar 400 quando o apelido excede o tamanho máximo")
        void shouldReturn400WhenApelidoTooLong() throws Exception {
            BranchRequest invalid = new BranchRequest(CNPJ_ALFANUMERICO_VALIDO, false, "x".repeat(101),
                    null, null, null, List.of());

            mockMvc.perform(post("/clients/{publicId}/branches", clientPublicId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve repassar o corpo com o documento já normalizado ao serviço")
        void shouldForwardNormalizedDocumentoToService() throws Exception {
            BranchRequest masked = new BranchRequest("12.abc.345/01de-35", false, null,
                    null, null, null, List.of());
            when(clientBranchService.createBranch(eq(clientPublicId), any(BranchRequest.class)))
                    .thenReturn(branchResponse);

            mockMvc.perform(post("/clients/{publicId}/branches", clientPublicId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(masked)))
                    .andExpect(status().isCreated());

            verify(clientBranchService).createBranch(clientPublicId,
                    new BranchRequest(CNPJ_ALFANUMERICO_VALIDO, false, null, null, null, null, List.of()));
        }
    }
}
