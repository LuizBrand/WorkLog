package br.com.luizbrand.worklog.client.integration;

import br.com.luizbrand.worklog.client.integration.dto.CnpjaOfficeResponse;
import br.com.luizbrand.worklog.exception.External.ExternalRateLimitException;
import br.com.luizbrand.worklog.exception.External.ExternalServiceUnavailableException;
import br.com.luizbrand.worklog.exception.NotFound.CnpjNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Exercita a desserialização real e a tradução dos status HTTP da CNPJá sem
 * sair para a rede: o {@link MockRestServiceServer} substitui a fábrica de
 * requisições do {@link RestClient}.
 */
class CnpjaClientTest {

    private static final String BASE_URL = "https://open.cnpja.com";
    private static final String CNPJ = "07526557011659";
    private static final String URL = BASE_URL + "/office/" + CNPJ;

    private MockRestServiceServer mockServer;
    private CnpjaClient cnpjaClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        cnpjaClient = new CnpjaClient(builder.build());
    }

    private String fixture() throws Exception {
        return new ClassPathResource("cnpja/office-ambev-manaus.json")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("200 -> desserializa o subset do contrato, ignorando o resto")
    void shouldDeserializeOfficeResponse() throws Exception {
        mockServer.expect(requestTo(URL))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(fixture(), MediaType.APPLICATION_JSON));

        CnpjaOfficeResponse office = cnpjaClient.findOffice(CNPJ);

        assertThat(office.taxId()).isEqualTo(CNPJ);
        assertThat(office.alias()).isEqualTo("AMBEV FILIAL MANAUS");
        assertThat(office.head()).isFalse();
        assertThat(office.company().name()).isEqualTo("AMBEV S.A.");
        assertThat(office.address().street()).isEqualTo("Avenida Buriti");
        assertThat(office.address().number()).isEqualTo("4000");
        assertThat(office.address().details()).isEqualTo("Galpao 2");
        assertThat(office.address().district()).isEqualTo("Distrito Industrial");
        assertThat(office.address().city()).isEqualTo("Manaus");
        assertThat(office.address().state()).isEqualTo("AM");
        assertThat(office.address().zip()).isEqualTo("69075000");
        assertThat(office.phones()).hasSize(2);
        assertThat(office.phones().get(0).type()).isEqualTo("LANDLINE");
        assertThat(office.phones().get(0).area()).isEqualTo("92");
        assertThat(office.phones().get(0).number()).isEqualTo("21238000");
        assertThat(office.emails()).hasSize(2);
        assertThat(office.emails().get(0).ownership()).isEqualTo("ACCOUNTING");
        assertThat(office.emails().get(0).address()).isEqualTo("contabilidade@ambev.com.br");

        mockServer.verify();
    }

    @Test
    @DisplayName("Campos que a CNPJá manda e nós não modelamos não podem quebrar a leitura")
    void shouldIgnoreUnknownFields() throws Exception {
        // A fixture traz members, CNAEs, capital social, registrations e status —
        // tudo fora do nosso subset. Sem ignoreUnknown, a leitura estouraria.
        mockServer.expect(requestTo(URL))
                .andRespond(withSuccess(fixture(), MediaType.APPLICATION_JSON));

        assertThat(cnpjaClient.findOffice(CNPJ)).isNotNull();
    }

    @Test
    @DisplayName("404 -> CnpjNotFoundException")
    void shouldTranslateNotFound() {
        mockServer.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .body("{\"code\":404,\"message\":\"Not Found\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> cnpjaClient.findOffice(CNPJ))
                .isInstanceOf(CnpjNotFoundException.class)
                .hasMessageContaining(CNPJ);
    }

    @Test
    @DisplayName("429 -> ExternalRateLimitException (limite de 5 consultas/min por IP)")
    void shouldTranslateTooManyRequests() {
        mockServer.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> cnpjaClient.findOffice(CNPJ))
                .isInstanceOf(ExternalRateLimitException.class);
    }

    @Test
    @DisplayName("503 -> ExternalServiceUnavailableException")
    void shouldTranslateServiceUnavailable() {
        mockServer.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> cnpjaClient.findOffice(CNPJ))
                .isInstanceOf(ExternalServiceUnavailableException.class);
    }

    @Test
    @DisplayName("500 -> ExternalServiceUnavailableException")
    void shouldTranslateInternalServerError() {
        mockServer.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> cnpjaClient.findOffice(CNPJ))
                .isInstanceOf(ExternalServiceUnavailableException.class);
    }

    @Test
    @DisplayName("400 da CNPJá também vira 503: é falha nossa ou deles, nunca do usuário")
    void shouldTranslateUnexpectedStatus() {
        // O documento já foi validado antes de chegar aqui, então um 400 da API
        // significa que o contrato mudou — não faz sentido devolver 400 ao front.
        mockServer.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> cnpjaClient.findOffice(CNPJ))
                .isInstanceOf(ExternalServiceUnavailableException.class);
    }

    @Test
    @DisplayName("Falha de conexão/timeout -> ExternalServiceUnavailableException")
    void shouldTranslateConnectionFailure() {
        mockServer.expect(requestTo(URL))
                .andRespond(request -> {
                    throw new java.net.SocketTimeoutException("Read timed out");
                });

        assertThatThrownBy(() -> cnpjaClient.findOffice(CNPJ))
                .isInstanceOf(ExternalServiceUnavailableException.class);
    }
}
