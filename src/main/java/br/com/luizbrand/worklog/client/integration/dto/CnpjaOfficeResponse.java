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
        List<Email> emails,
        Status status,
        List<Registration> registrations) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Company(String name, SimplesSimei simples, SimplesSimei simei) {
    }

    /**
     * Opção pelo Simples Nacional ou enquadramento no MEI. Só o {@code optant}
     * interessa: o histórico de períodos é da API comercial.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SimplesSimei(Boolean optant) {
    }

    /**
     * Situação cadastral. Os códigos do contrato são {@code 1} Nula,
     * {@code 2} Ativa, {@code 3} Suspensa, {@code 4} Inapta e {@code 8} Baixada.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Status(Integer id, String text) {

        public static final int ATIVA = 2;
    }

    /**
     * Inscrição estadual. Um mesmo estabelecimento pode ter várias — a IE é por
     * estabelecimento, e quem vende para outros estados se inscreve neles como
     * substituto tributário.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Registration(String number, String state, Boolean enabled, Type type) {

        /**
         * Códigos do contrato: {@code 1} IE Normal, {@code 2} Substituto
         * Tributário, {@code 3} Não Contribuinte, {@code 4} Contribuinte da UF
         * com endereço em outra, {@code 5} Produtor Rural, {@code 9} Não
         * informada.
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Type(Integer id, String text) {

            public static final int IE_NORMAL = 1;
        }
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
