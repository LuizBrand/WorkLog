package br.com.luizbrand.worklog.exception;

public class RoleNotFoundException extends RuntimeException{

    public RoleNotFoundException(String message) {
        super(message);
    }

}
