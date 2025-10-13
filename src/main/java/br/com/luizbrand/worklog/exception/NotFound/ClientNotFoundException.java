package br.com.luizbrand.worklog.exception.NotFound;

public class ClientNotFoundException extends ResourceNotFoundException {

    public ClientNotFoundException(String message) {
        super(message);
    }

}
