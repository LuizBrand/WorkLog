package br.com.luizbrand.worklog.exception.BadRequest;

/**
 * Documento (CPF/CNPJ) ausente ou reprovado no checksum → 400.
 */
public class InvalidDocumentoException extends InvalidRequestException {

    public InvalidDocumentoException(String message) {
        super(message);
    }
}
