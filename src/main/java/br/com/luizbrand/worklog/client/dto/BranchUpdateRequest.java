package br.com.luizbrand.worklog.client.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Semântica PATCH: todos os campos são opcionais e só os não nulos são
 * aplicados.
 *
 * <p>Dois campos têm tratamento especial e por isso não passam pelo mapper:
 * {@code isMatriz} (promover uma filial rebaixa a anterior, o que é regra de
 * serviço) e a ausência de {@code enabled} — inativar filial tem endpoint
 * próprio ({@code POST .../deactivate}), para o front deixar claro que é
 * inativação e não exclusão.
 *
 * <p>{@code contatos} ausente não altera nada; presente <b>substitui a lista
 * inteira</b> (decisão D7).
 */
public record BranchUpdateRequest(
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

    public BranchUpdateRequest {
        documento = BranchRequest.normalizeDocumento(documento);
    }
}
