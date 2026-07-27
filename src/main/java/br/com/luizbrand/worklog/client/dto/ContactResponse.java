package br.com.luizbrand.worklog.client.dto;

import br.com.luizbrand.worklog.client.enums.ContactType;

import java.util.UUID;

public record ContactResponse(
        UUID publicId,
        ContactType tipo,
        String valor,
        String descricao,
        boolean principal) {
}
