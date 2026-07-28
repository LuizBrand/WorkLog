package br.com.luizbrand.worklog.exceptionhandler;

import br.com.luizbrand.worklog.exception.Business.BusinessException;
import br.com.luizbrand.worklog.exception.Business.RefreshTokenException;
import br.com.luizbrand.worklog.exception.Conflict.ResourceAlreadyExistsException;
import br.com.luizbrand.worklog.exception.NotFound.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ApiExceptionResponse> handleEmailAlreadyExists(ResourceAlreadyExistsException ex, WebRequest request) {

        ApiExceptionResponse exceptionResponse = ApiExceptionResponse.builder()
                .mediaType(MediaType.APPLICATION_JSON)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return new ResponseEntity<>(exceptionResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiExceptionResponse> handleContentNotFound(ResourceNotFoundException ex, WebRequest request) {

        ApiExceptionResponse exceptionResponse = ApiExceptionResponse.builder()
                .mediaType(MediaType.APPLICATION_JSON)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return new ResponseEntity<>(exceptionResponse, HttpStatus.NOT_FOUND);

    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiExceptionResponse> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        ApiExceptionResponse exceptionResponse = ApiExceptionResponse.builder()
                .mediaType(MediaType.APPLICATION_JSON)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Validation Exception. Check the fields")
                .path(request.getDescription(false).replace("uri=", ""))
                .fieldErrors(fieldErrors)
                .build();

        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiExceptionResponse> handleBusinessException(BusinessException ex, WebRequest request) {
        ApiExceptionResponse exceptionResponse = ApiExceptionResponse.builder()
                .mediaType(MediaType.APPLICATION_JSON)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .error("Business role exception")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return new ResponseEntity<>(exceptionResponse, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Fecha a janela de corrida das checagens de unicidade feitas em serviço:
     * duas requisições simultâneas passam as duas pelo {@code existsBy...} e a
     * segunda estoura na constraint do banco — que sem este handler viraria 500.
     *
     * <p>Cuidados desta escolha, que é <b>global</b>:
     * <ul>
     *   <li>A mensagem devolvida é genérica de propósito. A original traz nome de
     *       constraint, tabela e coluna — detalhe de schema que não deve vazar.</li>
     *   <li>Nem toda violação de integridade é conflito do cliente (violação de FK,
     *       por exemplo, costuma ser bug do servidor). Por isso a causa real é
     *       logada em WARN: sem isso, um bug de servidor viraria um 409 silencioso
     *       e sem rastro.</li>
     * </ul>
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiExceptionResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {

        logger.warn("Violação de integridade tratada como 409 em {}", request.getDescription(false), ex);

        ApiExceptionResponse exceptionResponse = ApiExceptionResponse.builder()
                .mediaType(MediaType.APPLICATION_JSON)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message("A operação viola uma restrição de integridade dos dados")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return new ResponseEntity<>(exceptionResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(RefreshTokenException.class)
    public ResponseEntity<ApiExceptionResponse> handleRefresTokenExcpetion(RefreshTokenException ex, WebRequest request) {
        ApiExceptionResponse exceptionResponse = ApiExceptionResponse.builder()
                .mediaType(MediaType.APPLICATION_JSON)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return new ResponseEntity<>(exceptionResponse, HttpStatus.UNAUTHORIZED);
    }

}
