package br.com.luizbrand.worklog.exception.Conflict;

public class EmailAlreadyExistsException extends ResourceAlreadyExistsException {

    public EmailAlreadyExistsException(String message) {
        super(message);
    }

}
