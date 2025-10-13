package br.com.luizbrand.worklog.exception.Conflict;

public class ClientAlreadyExistsException extends ResourceAlreadyExistsException {

    public ClientAlreadyExistsException(String message) {
        super(message);
    }


}
