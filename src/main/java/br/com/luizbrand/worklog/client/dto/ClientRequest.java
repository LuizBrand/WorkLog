package br.com.luizbrand.worklog.client.dto;

import br.com.luizbrand.worklog.client.enums.ClientType;
import br.com.luizbrand.worklog.client.enums.RegimeTributario;
import br.com.luizbrand.worklog.client.validation.ValidDocumento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@ValidDocumento
public record ClientRequest(
        @NotNull(message = "O tipo do cliente é obrigatório")
        ClientType tipo,
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String name,
        @Size(max = 100, message = "O nome fantasia deve ter no máximo 100 caracteres")
        String nomeFantasia,
        RegimeTributario regimeTributario,
        // Associar sistema é opcional: um cliente pode ser cadastrado antes de se
        // saber quais produtos ele usa, e um PF frequentemente não usa nenhum.
        List<UUID> systemsPublicIds,
        @Valid
        @NotEmpty(message = "Informe ao menos a matriz")
        List<BranchRequest> branches,
        Boolean enabled) {
}
