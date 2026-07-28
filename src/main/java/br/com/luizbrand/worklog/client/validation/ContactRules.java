package br.com.luizbrand.worklog.client.validation;

import br.com.luizbrand.worklog.client.dto.ContactRequest;

import java.util.List;

/**
 * Regras de contato que valem em todos os caminhos que criam ou substituem a
 * lista: {@code POST /clients}, {@code POST /branches} e {@code PATCH /branches}.
 * Fica aqui, e não num serviço, porque {@code ClientBranchService} já depende de
 * {@code ClientService} — a dependência inversa fecharia um ciclo de bean.
 */
public final class ContactRules {

    private ContactRules() {
    }

    /**
     * Uma filial tem no máximo um contato principal. Devolve boolean em vez de
     * lançar, no mesmo estilo do {@link DocumentoChecksum}: quem chama é que
     * escolhe a exceção.
     */
    public static boolean hasMultiplePrincipais(List<ContactRequest> contatos) {
        if (contatos == null) {
            return false;
        }
        return contatos.stream()
                .filter(contato -> contato != null && Boolean.TRUE.equals(contato.principal()))
                .count() > 1;
    }

    public static final String MENSAGEM_MULTIPLOS_PRINCIPAIS =
            "A filial deve ter no máximo um contato principal";
}
