package br.com.luizbrand.worklog.client.validation;

import br.com.luizbrand.worklog.client.dto.BranchRequest;
import br.com.luizbrand.worklog.client.dto.ClientRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

/**
 * Valida o documento de cada filial do {@link ClientRequest}. O algoritmo em si
 * mora em {@link DocumentoChecksum}, compartilhado com o
 * {@code ClientBranchService} — o sub-recurso de filiais não passa por aqui.
 */
public class DocumentoValidator implements ConstraintValidator<ValidDocumento, ClientRequest> {

    @Override
    public boolean isValid(ClientRequest request, ConstraintValidatorContext context) {
        if (request == null || request.tipo() == null || request.branches() == null) {
            return true;
        }

        List<BranchRequest> branches = request.branches();
        boolean valido = true;

        for (int i = 0; i < branches.size(); i++) {
            BranchRequest branch = branches.get(i);

            // Documento nulo é aceito aqui: a obrigatoriedade é regra de negócio
            // (fase 3) e as filiais de legado nasceram sem documento.
            if (branch == null || branch.documento() == null) {
                continue;
            }
            if (DocumentoChecksum.isValid(request.tipo(), branch.documento())) {
                continue;
            }

            if (valido) {
                context.disableDefaultConstraintViolation();
                valido = false;
            }
            // Aponta o erro para a filial certa, senão o front recebe um erro
            // solto no objeto raiz.
            context.buildConstraintViolationWithTemplate(DocumentoChecksum.mensagemPara(request.tipo()))
                    .addPropertyNode("branches")
                    .addPropertyNode("documento").inIterable().atIndex(i)
                    .addConstraintViolation();
        }

        return valido;
    }
}
