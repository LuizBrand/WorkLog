package br.com.luizbrand.worklog.client.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valida o documento de cada filial de acordo com o tipo do cliente:
 * CPF quando PF, CNPJ (numérico ou alfanumérico) quando PJ.
 * <p>
 * É uma constraint de classe porque o documento mora em {@code BranchRequest},
 * mas o discriminador ({@code tipo}) mora em {@code ClientRequest}.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DocumentoValidator.class)
public @interface ValidDocumento {

    String message() default "Documento inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
