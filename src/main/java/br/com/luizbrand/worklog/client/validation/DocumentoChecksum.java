package br.com.luizbrand.worklog.client.validation;

import br.com.luizbrand.worklog.client.enums.ClientType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.constraints.br.CNPJ;
import org.hibernate.validator.constraints.br.CPF;

/**
 * Checksum de CPF/CNPJ isolado do Bean Validation para poder ser chamado também
 * pelo serviço — o sub-recurso de filiais recebe um {@code BranchRequest} solto,
 * que não conhece o {@code tipo} do cliente, então a constraint de classe
 * {@code @ValidDocumento} não alcança aquele caminho.
 *
 * <p>Não existe API pública do hibernate-validator para chamar o algoritmo
 * direto (as classes são internas), então a verificação é delegada validando um
 * record "holder" anotado. O {@link ValidatorFactory} é próprio e estático —
 * injetar o Validator do Spring aqui criaria risco de ciclo de bean.
 */
public final class DocumentoChecksum {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    private DocumentoChecksum() {
    }

    /**
     * Documento nulo é considerado válido: a obrigatoriedade é regra de negócio
     * de cada caminho, e as filiais de legado nasceram sem documento.
     */
    public static boolean isValid(ClientType tipo, String documento) {
        if (tipo == null || documento == null) {
            return true;
        }
        return tipo == ClientType.PF
                ? VALIDATOR.validate(new CpfHolder(documento)).isEmpty()
                : VALIDATOR.validate(new CnpjHolder(documento)).isEmpty();
    }

    public static String mensagemPara(ClientType tipo) {
        return tipo == ClientType.PF ? "CPF inválido" : "CNPJ inválido";
    }

    // O format ALPHANUMERIC também aceita CNPJ 100% numérico, então uma única
    // anotação cobre os dois formatos da transição de 2026.
    record CnpjHolder(@CNPJ(format = CNPJ.Format.ALPHANUMERIC) String documento) {
    }

    record CpfHolder(@CPF String documento) {
    }
}
