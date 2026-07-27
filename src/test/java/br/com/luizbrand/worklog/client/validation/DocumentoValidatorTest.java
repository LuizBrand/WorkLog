package br.com.luizbrand.worklog.client.validation;

import br.com.luizbrand.worklog.client.dto.BranchRequest;
import br.com.luizbrand.worklog.client.dto.ClientRequest;
import br.com.luizbrand.worklog.client.enums.ClientType;
import br.com.luizbrand.worklog.support.ClientRequestTestBuilder;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.CNPJ_ALFANUMERICO_VALIDO;
import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.CNPJ_VALIDO;
import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.CPF_VALIDO;
import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.aClientRequest;
import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.filial;
import static br.com.luizbrand.worklog.support.ClientRequestTestBuilder.matriz;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("@ValidDocumento / DocumentoValidator")
class DocumentoValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        factory.close();
    }

    @Test
    @DisplayName("PF com CPF válido não gera violação")
    void shouldAcceptValidCpfForPessoaFisica() {
        Set<ConstraintViolation<ClientRequest>> violations = validate(
                requestFor(ClientType.PF, CPF_VALIDO));

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("PF com CPF de checksum inválido gera violação em branches[0].documento")
    void shouldRejectCpfWithBadChecksum() {
        Set<ConstraintViolation<ClientRequest>> violations = validate(
                requestFor(ClientType.PF, "11144477700"));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath())
                .hasToString("branches[0].documento");
    }

    @Test
    @DisplayName("PJ com CNPJ numérico válido não gera violação")
    void shouldAcceptValidNumericCnpj() {
        Set<ConstraintViolation<ClientRequest>> violations = validate(
                requestFor(ClientType.PJ, CNPJ_VALIDO));

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("PJ com CNPJ alfanumérico válido não gera violação")
    void shouldAcceptValidAlphanumericCnpj() {
        Set<ConstraintViolation<ClientRequest>> violations = validate(
                requestFor(ClientType.PJ, CNPJ_ALFANUMERICO_VALIDO));

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("PJ com CPF no lugar do CNPJ gera violação")
    void shouldRejectCpfWhenClientIsPessoaJuridica() {
        Set<ConstraintViolation<ClientRequest>> violations = validate(
                requestFor(ClientType.PJ, CPF_VALIDO));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath())
                .hasToString("branches[0].documento");
    }

    @Test
    @DisplayName("Documento nulo é aceito nesta fase (obrigatoriedade é regra de negócio)")
    void shouldAcceptNullDocumento() {
        assertThat(validate(requestFor(ClientType.PJ, null))).isEmpty();
        assertThat(validate(requestFor(ClientType.PF, null))).isEmpty();
    }

    @Test
    @DisplayName("Documento com máscara é aceito e chega normalizado no record")
    void shouldNormalizeMaskedDocumento() {
        ClientRequest request = requestFor(ClientType.PJ, "12.345.678/0001-95");

        assertThat(validate(request)).isEmpty();
        assertThat(request.branches().get(0).documento()).isEqualTo(CNPJ_VALIDO);
    }

    @Test
    @DisplayName("Documento alfanumérico em minúsculas é normalizado para maiúsculas e aceito")
    void shouldUppercaseAlphanumericDocumento() {
        ClientRequest request = requestFor(ClientType.PJ, "12.abc.345/01de-35");

        assertThat(validate(request)).isEmpty();
        assertThat(request.branches().get(0).documento()).isEqualTo(CNPJ_ALFANUMERICO_VALIDO);
    }

    @Test
    @DisplayName("Com duas filiais, a violação aponta o índice da filial inválida")
    void shouldPointToTheInvalidBranchIndex() {
        ClientRequest request = aClientRequest()
                .withTipo(ClientType.PJ)
                .withSystem(UUID.randomUUID())
                .withBranches(matriz(CNPJ_VALIDO), filial("12345678000100"))
                .build();

        Set<ConstraintViolation<ClientRequest>> violations = validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath())
                .hasToString("branches[1].documento");
    }

    @Test
    @DisplayName("Documento em branco é normalizado para null e aceito")
    void shouldTreatBlankDocumentoAsNull() {
        ClientRequest request = requestFor(ClientType.PJ, "   ");

        assertThat(validate(request)).isEmpty();
        assertThat(request.branches().get(0).documento()).isNull();
    }

    private static ClientRequest requestFor(ClientType tipo, String documento) {
        return aClientRequest()
                .withTipo(tipo)
                .withSystem(UUID.randomUUID())
                .withBranches(List.<BranchRequest>of(matriz(documento)))
                .build();
    }

    private static Set<ConstraintViolation<ClientRequest>> validate(ClientRequest request) {
        return validator.validate(request);
    }
}
