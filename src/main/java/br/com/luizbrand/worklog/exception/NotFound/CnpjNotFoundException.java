package br.com.luizbrand.worklog.exception.NotFound;

/**
 * CNPJ não registrado na Receita Federal → 404.
 */
public class CnpjNotFoundException extends ResourceNotFoundException {

    public CnpjNotFoundException(String message) {
        super(message);
    }
}
