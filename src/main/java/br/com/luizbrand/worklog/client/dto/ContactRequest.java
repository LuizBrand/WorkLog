package br.com.luizbrand.worklog.client.dto;

import br.com.luizbrand.worklog.client.enums.ContactType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Contato de uma filial. O formato do {@code valor} não é validado por tipo
 * nesta fase (decisão D4) — apenas obrigatoriedade e tamanho.
 */
public record ContactRequest(
        @NotNull(message = "O tipo do contato é obrigatório")
        ContactType tipo,
        @NotBlank(message = "O valor do contato é obrigatório")
        @Size(max = 150, message = "O valor do contato deve ter no máximo 150 caracteres")
        String valor,
        @Size(max = 100, message = "A descrição do contato deve ter no máximo 100 caracteres")
        String descricao,
        Boolean principal) {
}
