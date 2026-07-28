package br.com.luizbrand.worklog.exception.External;

/**
 * Serviço externo indisponível, com erro interno ou fora do prazo → 503.
 *
 * <p>Não herda de {@code BusinessException}: nenhuma regra de negócio foi
 * violada, a falha é de infraestrutura de terceiro.
 */
public class ExternalServiceUnavailableException extends RuntimeException {

    public ExternalServiceUnavailableException(String message) {
        super(message);
    }

    public ExternalServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
