package br.com.luizbrand.worklog.client.dto;

import jakarta.validation.constraints.Size;

import java.util.Locale;

/**
 * Endereço da filial. Todos os campos são opcionais — filiais de legado e
 * pessoas físicas podem nascer sem endereço.
 */
public record AddressRequest(
        @Size(max = 8, message = "O CEP deve ter no máximo 8 dígitos")
        String cep,
        @Size(max = 150, message = "O logradouro deve ter no máximo 150 caracteres")
        String logradouro,
        @Size(max = 20, message = "O número deve ter no máximo 20 caracteres")
        String numero,
        @Size(max = 100, message = "O complemento deve ter no máximo 100 caracteres")
        String complemento,
        @Size(max = 100, message = "O bairro deve ter no máximo 100 caracteres")
        String bairro,
        @Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres")
        String cidade,
        @Size(max = 2, message = "A UF deve ter 2 caracteres")
        String uf) {

    public AddressRequest {
        // Normaliza antes da validação: CEP só com dígitos e UF em maiúsculas.
        cep = cep == null ? null : cep.replaceAll("\\D", "");
        uf = uf == null ? null : uf.toUpperCase(Locale.ROOT);
    }
}
