package br.com.luizbrand.worklog.client.dto;

import br.com.luizbrand.worklog.client.enums.ClientType;
import br.com.luizbrand.worklog.client.enums.RegimeTributario;
import br.com.luizbrand.worklog.system.dto.SystemResponse;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ClientResponse(
        UUID publicId,
        ClientType tipo,
        String name,
        String nomeFantasia,
        RegimeTributario regimeTributario,
        boolean enabled,
        OffsetDateTime createdAt,
        List<SystemResponse> systems,
        List<BranchResponse> branches) {
}
