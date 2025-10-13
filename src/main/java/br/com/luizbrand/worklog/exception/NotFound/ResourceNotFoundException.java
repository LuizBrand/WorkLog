package br.com.luizbrand.worklog.exception.NotFound;

public class ResourceNotFoundException extends RuntimeException{

    public ResourceNotFoundException(String message) {
        super(message);
    }

}
