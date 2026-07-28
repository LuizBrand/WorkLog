package br.com.luizbrand.worklog.exception.NotFound;

public class BranchNotFoundException extends ResourceNotFoundException {

    public BranchNotFoundException(String message) {
        super(message);
    }

}
