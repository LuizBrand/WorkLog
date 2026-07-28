package br.com.luizbrand.worklog.client.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Contrato <b>externo</b> da CNPJá: subset de {@code GET /office/{taxId}} com os
 * campos que realmente usamos.
 *
 * <p>Existe separado do {@code CnpjLookupResponse} de propósito. Este record
 * pertence à CNPJá e pode mudar sem aviso; aquele pertence ao nosso frontend.
 * A tradução entre os dois é o único ponto que quebra se o contrato deles mudar.
 *
 * <p>O {@code ignoreUnknown} não é decoração: a resposta traz quadro societário,
 * CNAEs, capital social, natureza jurídica e SUFRAMA, todos fora do nosso
 * modelo. Sem ele, a leitura estouraria em campo que não nos interessa.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CnpjaOfficeResponse(
        String taxId,
        String alias,
        Boolean head,
        Company company,
        Address address,
        List<Phone> phones,
        List<Email> emails) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Company(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Address(
            String street,
            String number,
            String details,
            String district,
            String city,
            String state,
            String zip) {
    }

    /**
     * {@code type} é {@code LANDLINE} ou {@code MOBILE} no contrato deles.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Phone(String type, String area, String number) {
    }

    /**
     * {@code ownership} é {@code ACCOUNTING}, {@code CORPORATE} ou
     * {@code PERSONAL} no contrato deles.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Email(String ownership, String address) {
    }
}
