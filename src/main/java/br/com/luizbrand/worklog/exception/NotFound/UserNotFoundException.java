package br.com.luizbrand.worklog.exception.NotFound;

public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException(String message) {
        super(message);
    }

}
