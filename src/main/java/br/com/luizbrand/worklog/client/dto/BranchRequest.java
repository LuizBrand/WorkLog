package br.com.luizbrand.worklog.client.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Locale;

/**
 * Filial (matriz ou não) de um cliente. O {@code documento} é validado no nível
 * do {@link ClientRequest} via {@code @ValidDocumento}, porque a escolha entre
 * CPF e CNPJ depende do {@code tipo} do cliente — que a filial não conhece.
 */
public record BranchRequest(
        String documento,
        Boolean isMatriz,
        @Size(max = 100, message = "O apelido deve ter no máximo 100 caracteres")
        String apelido,
        @Size(max = 20, message = "A inscrição estadual deve ter no máximo 20 caracteres")
        String inscricaoEstadual,
        @Size(max = 20, message = "A inscrição municipal deve ter no máximo 20 caracteres")
        String inscricaoMunicipal,
        @Valid AddressRequest address,
        @Valid List<ContactRequest> contatos) {

    public BranchRequest {
        // Normaliza antes da validação e da persistência (decisão D3).
        documento = normalizeDocumento(documento);
    }

    /**
     * Remove máscara e converte para maiúsculas. O CNPJ alfanumérico só é válido
     * em maiúsculas, então a normalização é obrigatória, não estética.
     */
    public static String normalizeDocumento(String raw) {
        if (raw == null) {
            return null;
        }
        String limpo = raw.replaceAll("[^0-9A-Za-z]", "").toUpperCase(Locale.ROOT);
        return limpo.isEmpty() ? null : limpo;
    }
}
