package br.com.luizbrand.worklog.exception.Business;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
