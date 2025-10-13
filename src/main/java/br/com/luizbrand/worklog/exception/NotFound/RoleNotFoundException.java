package br.com.luizbrand.worklog.exception.NotFound;

public class RoleNotFoundException extends ResourceNotFoundException {

    public RoleNotFoundException(String message) {
        super(message);
    }

}
