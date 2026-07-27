package br.com.luizbrand.worklog.client.dto;

public record AddressResponse(
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf) {
}
