package br.com.luizbrand.worklog.client.integration;

import br.com.luizbrand.worklog.client.integration.dto.CnpjaOfficeResponse;
import br.com.luizbrand.worklog.exception.External.ExternalRateLimitException;
import br.com.luizbrand.worklog.exception.External.ExternalServiceUnavailableException;
import br.com.luizbrand.worklog.exception.NotFound.CnpjNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Acesso à API pública da CNPJá ({@code GET /office/{taxId}}).
 *
 * <p>Única responsabilidade além da chamada: <b>traduzir o erro HTTP deles em
 * exceção nossa</b>, para que nada acima desta classe precise conhecer o
 * contrato de terceiro.
 *
 * <p>A API pública não exige autenticação, mas limita a <b>5 consultas por
 * minuto por endereço IP</b> — compartilhado por toda a equipe quando o backend
 * roda numa VPS. Por isso o 429 é um caminho esperado, não uma anomalia.
 */
@Component
public class CnpjaClient {

    private static final Logger logger = LoggerFactory.getLogger(CnpjaClient.class);

    private static final String INDISPONIVEL =
            "Serviço de consulta de CNPJ indisponível no momento. Preencha o cadastro manualmente.";

    private final RestClient restClient;

    public CnpjaClient(RestClient cnpjaRestClient) {
        this.restClient = cnpjaRestClient;
    }

    public CnpjaOfficeResponse findOffice(String cnpj) {
        try {
            CnpjaOfficeResponse office = restClient.get()
                    .uri("/office/{cnpj}", cnpj)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw traduzir(response.getStatusCode(), cnpj);
                    })
                    .body(CnpjaOfficeResponse.class);

            if (office == null) {
                // 200 com corpo vazio não deveria acontecer, mas devolver null
                // daqui estouraria como NullPointerException lá no mapeamento.
                throw new ExternalServiceUnavailableException(INDISPONIVEL);
            }

            return office;
        } catch (ResourceAccessException e) {
            // Timeout de conexão ou de leitura, DNS, conexão recusada.
            throw new ExternalServiceUnavailableException(INDISPONIVEL, e);
        }
    }

    private RuntimeException traduzir(HttpStatusCode status, String cnpj) {
        if (status.isSameCodeAs(HttpStatus.NOT_FOUND)) {
            return new CnpjNotFoundException("CNPJ não encontrado na Receita Federal: " + cnpj);
        }

        if (status.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
            return new ExternalRateLimitException(
                    "Limite de consultas de CNPJ atingido. Aguarde um minuto e tente novamente.");
        }

        // Qualquer outro status é falha do provedor ou do nosso uso do contrato
        // dele — o documento já foi validado antes de chegar aqui, então não faz
        // sentido devolver 400 ao usuário. Fica registrado para investigação.
        logger.warn("Consulta de CNPJ falhou na CNPJá com status {} para o documento {}", status, cnpj);
        return new ExternalServiceUnavailableException(INDISPONIVEL);
    }
}
