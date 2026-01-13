package br.com.luizbrand.worklog.exception.NotFound;

public class TicketNotFoundException extends ResourceNotFoundException {
    public TicketNotFoundException(String message) {
        super(message);
    }
}
