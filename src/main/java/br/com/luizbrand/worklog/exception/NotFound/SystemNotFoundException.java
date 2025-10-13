package br.com.luizbrand.worklog.exception.NotFound;

public class SystemNotFoundException extends ResourceNotFoundException {

    public SystemNotFoundException(String message) {
        super(message);
    }

}
