package br.com.luizbrand.worklog.exception.External;

/**
 * Limite de uso de um serviço externo foi estourado → 429 repassado ao cliente.
 *
 * <p>Não herda de {@code BusinessException} de propósito: cairia no handler de
 * 422 e o front perderia a informação de que basta esperar e tentar de novo.
 */
public class ExternalRateLimitException extends RuntimeException {

    public ExternalRateLimitException(String message) {
        super(message);
    }
}
