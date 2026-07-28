package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.auth.AuthFilter;
import br.com.luizbrand.worklog.auth.CustomUserDetailsService;
import br.com.luizbrand.worklog.client.dto.AddressRequest;
import br.com.luizbrand.worklog.client.dto.BranchRequest;
import br.com.luizbrand.worklog.client.dto.CnpjLookupResponse;
import br.com.luizbrand.worklog.client.dto.ContactRequest;
import br.com.luizbrand.worklog.client.enums.ClientType;
import br.com.luizbrand.worklog.client.enums.ContactType;
import br.com.luizbrand.worklog.exception.BadRequest.InvalidDocumentoException;
import br.com.luizbrand.worklog.exception.External.ExternalRateLimitException;
import br.com.luizbrand.worklog.exception.External.ExternalServiceUnavailableException;
import br.com.luizbrand.worklog.exception.NotFound.CnpjNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest({ClientLookupController.class, ClientController.class})
@AutoConfigureMockMvc(addFilters = false)
class ClientLookupControllerTest {

    private static final String CNPJ = "07526557011659";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CnpjLookupService cnpjLookupService;

    @MockitoBean
    private ClientService clientService;

    @MockitoBean
    private AuthFilter authFilter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private CnpjLookupResponse resposta() {
        return new CnpjLookupResponse(
                ClientType.PJ,
                "AMBEV S.A.",
                null,
                List.of(new BranchRequest(CNPJ, false, "AMBEV FILIAL MANAUS", null, null,
                        new AddressRequest("69075000", "Avenida Buriti", "4000", "Galpao 2",
                                "Distrito Industrial", "Manaus", "AM"),
                        List.of(new ContactRequest(ContactType.EMAIL, "contabilidade@ambev.com.br",
                                "Contabilidade", null)))));
    }

    @Test
    @DisplayName("GET /clients/lookup -> 200 no formato do formulário de cadastro")
    void shouldReturnLookupResponse() throws Exception {
        when(cnpjLookupService.lookup(CNPJ)).thenReturn(resposta());

        mockMvc.perform(get("/clients/lookup").param("documento", CNPJ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("PJ"))
                .andExpect(jsonPath("$.name").value("AMBEV S.A."))
                .andExpect(jsonPath("$.nomeFantasia").doesNotExist())
                .andExpect(jsonPath("$.branches[0].documento").value(CNPJ))
                .andExpect(jsonPath("$.branches[0].apelido").value("AMBEV FILIAL MANAUS"))
                .andExpect(jsonPath("$.branches[0].isMatriz").value(false))
                .andExpect(jsonPath("$.branches[0].address.cidade").value("Manaus"))
                .andExpect(jsonPath("$.branches[0].address.uf").value("AM"))
                .andExpect(jsonPath("$.branches[0].contatos[0].tipo").value("EMAIL"));

        verify(cnpjLookupService).lookup(CNPJ);
    }

    @Test
    @DisplayName("/clients/lookup não é capturado pela rota /clients/{publicId}")
    void shouldNotCollideWithFindByPublicIdRoute() throws Exception {
        when(cnpjLookupService.lookup(CNPJ)).thenReturn(resposta());

        mockMvc.perform(get("/clients/lookup").param("documento", CNPJ))
                .andExpect(status().isOk());

        // O segmento literal precisa vencer o template {publicId}, senão a
        // requisição morre convertendo "lookup" para UUID.
        verify(cnpjLookupService).lookup(CNPJ);
        org.mockito.Mockito.verifyNoInteractions(clientService);
    }

    @Test
    @DisplayName("Documento inválido -> 400")
    void shouldReturn400ForInvalidDocumento() throws Exception {
        when(cnpjLookupService.lookup(anyString()))
                .thenThrow(new InvalidDocumentoException("CNPJ inválido"));

        mockMvc.perform(get("/clients/lookup").param("documento", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("CNPJ inválido"));
    }

    @Test
    @DisplayName("Sem o parâmetro documento -> 400 pelo mesmo caminho de erro")
    void shouldReturn400WhenDocumentoIsMissing() throws Exception {
        when(cnpjLookupService.lookup(null))
                .thenThrow(new InvalidDocumentoException("O CNPJ é obrigatório"));

        mockMvc.perform(get("/clients/lookup"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O CNPJ é obrigatório"));
    }

    @Test
    @DisplayName("CNPJ inexistente na Receita -> 404")
    void shouldReturn404ForUnknownCnpj() throws Exception {
        when(cnpjLookupService.lookup(CNPJ))
                .thenThrow(new CnpjNotFoundException("CNPJ não encontrado: " + CNPJ));

        mockMvc.perform(get("/clients/lookup").param("documento", CNPJ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("Limite da CNPJá estourado -> 429 repassado ao front")
    void shouldReturn429WhenRateLimited() throws Exception {
        when(cnpjLookupService.lookup(CNPJ))
                .thenThrow(new ExternalRateLimitException("Limite de consultas atingido"));

        mockMvc.perform(get("/clients/lookup").param("documento", CNPJ))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value("Limite de consultas atingido"));
    }

    @Test
    @DisplayName("CNPJá indisponível -> 503")
    void shouldReturn503WhenProviderIsDown() throws Exception {
        when(cnpjLookupService.lookup(CNPJ))
                .thenThrow(new ExternalServiceUnavailableException("Consulta indisponível"));

        mockMvc.perform(get("/clients/lookup").param("documento", CNPJ))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"));
    }

    @Test
    @DisplayName("A consulta não persiste nada: o cliente só nasce no POST /clients")
    void shouldNotTouchClientService() throws Exception {
        when(cnpjLookupService.lookup(CNPJ)).thenReturn(resposta());

        mockMvc.perform(get("/clients/lookup").param("documento", CNPJ))
                .andExpect(status().isOk());

        org.mockito.Mockito.verifyNoInteractions(clientService);
    }

    @Test
    @DisplayName("O publicId continua resolvendo normalmente na rota irmã")
    void shouldStillResolveClientByPublicId() throws Exception {
        UUID publicId = UUID.randomUUID();
        when(clientService.getClientByPublicId(publicId)).thenReturn(null);

        mockMvc.perform(get("/clients/" + publicId))
                .andExpect(status().isOk());

        verify(clientService).getClientByPublicId(publicId);
    }
}
