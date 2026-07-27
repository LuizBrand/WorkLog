package br.com.luizbrand.worklog.client.validation;

import br.com.luizbrand.worklog.client.dto.BranchRequest;
import br.com.luizbrand.worklog.client.dto.ClientRequest;
import br.com.luizbrand.worklog.client.enums.ClientType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.constraints.br.CNPJ;
import org.hibernate.validator.constraints.br.CPF;

import java.util.List;

/**
 * Não existe API pública do hibernate-validator para chamar o checksum de
 * {@code @CPF}/{@code @CNPJ} direto (as classes são internas), então a
 * verificação é delegada validando um record "holder" anotado. O
 * {@link ValidatorFactory} é próprio e estático — injetar o Validator do Spring
 * aqui criaria risco de ciclo de bean.
 */
public class DocumentoValidator implements ConstraintValidator<ValidDocumento, ClientRequest> {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

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
            if (documentoValido(request.tipo(), branch.documento())) {
                continue;
            }

            if (valido) {
                context.disableDefaultConstraintViolation();
                valido = false;
            }
            // Aponta o erro para a filial certa, senão o front recebe um erro
            // solto no objeto raiz.
            context.buildConstraintViolationWithTemplate(mensagemPara(request.tipo()))
                    .addPropertyNode("branches")
                    .addPropertyNode("documento").inIterable().atIndex(i)
                    .addConstraintViolation();
        }

        return valido;
    }

    private boolean documentoValido(ClientType tipo, String documento) {
        return tipo == ClientType.PF
                ? VALIDATOR.validate(new CpfHolder(documento)).isEmpty()
                : VALIDATOR.validate(new CnpjHolder(documento)).isEmpty();
    }

    private String mensagemPara(ClientType tipo) {
        return tipo == ClientType.PF ? "CPF inválido" : "CNPJ inválido";
    }

    // O format ALPHANUMERIC também aceita CNPJ 100% numérico, então uma única
    // anotação cobre os dois formatos da transição de 2026.
    record CnpjHolder(@CNPJ(format = CNPJ.Format.ALPHANUMERIC) String documento) {
    }

    record CpfHolder(@CPF String documento) {
    }
}
