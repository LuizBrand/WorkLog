package br.com.luizbrand.worklog.exception.Conflict;

public class SystemAlreadyExistsException extends ResourceAlreadyExistsException {

    public SystemAlreadyExistsException(String message) {
        super(message);
    }

}
