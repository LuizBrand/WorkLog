package br.com.luizbrand.worklog.exception.BadRequest;

/**
 * Requisição malformada detectada em serviço, e não pelo Bean Validation → 400.
 *
 * <p>Existe porque nem toda validação de entrada cabe numa anotação: a consulta
 * de CNPJ valida o checksum dentro do serviço, antes de gastar uma das cinco
 * requisições por minuto que a API pública da CNPJá permite.
 *
 * <p>Como o {@code RestExceptionHandler} captura pela classe base, novas
 * subclasses são tratadas automaticamente.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
