package br.com.luizbrand.worklog.exception.Conflict;

public class DocumentoAlreadyExistsException extends ResourceAlreadyExistsException {

    public DocumentoAlreadyExistsException(String message) {
        super(message);
    }

}
