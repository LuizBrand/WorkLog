package br.com.luizbrand.worklog.exception;

public class SystemAlreadyExistsException extends RuntimeException {

    public SystemAlreadyExistsException(String message) {
        super(message);
    }

}
