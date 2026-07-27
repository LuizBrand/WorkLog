package br.com.luizbrand.worklog.client.dto;

import br.com.luizbrand.worklog.client.enums.ClientType;
import br.com.luizbrand.worklog.client.enums.RegimeTributario;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Semântica PATCH: todos os campos são opcionais e só os não nulos são
 * aplicados. Filiais não são alteradas por aqui — elas têm sub-recurso próprio.
 */
public record ClientUpdateRequest(
        ClientType tipo,
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String name,
        @Size(max = 100, message = "O nome fantasia deve ter no máximo 100 caracteres")
        String nomeFantasia,
        RegimeTributario regimeTributario,
        List<UUID> systemsPublicIds,
        Boolean enabled) {
}
