package br.com.luizbrand.worklog.client.dto;

import java.util.List;
import java.util.UUID;

public record BranchResponse(
        UUID publicId,
        String documento,
        boolean isMatriz,
        String apelido,
        String inscricaoEstadual,
        String inscricaoMunicipal,
        AddressResponse address,
        List<ContactResponse> contatos,
        boolean enabled) {
}
