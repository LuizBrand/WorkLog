package br.com.luizbrand.worklog.client.integration;

import br.com.luizbrand.worklog.client.integration.dto.CnpjaOfficeResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CnpjaCacheTest {

    private static final String CNPJ = "07526557011659";

    @Mock
    private CachedOfficeRepository repository;

    private CnpjaCache cnpjaCache;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        CnpjaProperties properties = new CnpjaProperties("https://open.cnpja.com",
                Duration.ofSeconds(3), Duration.ofSeconds(10), Duration.ofHours(24));
        cnpjaCache = new CnpjaCache(repository, properties, objectMapper);
    }

    private CnpjaOfficeResponse office() {
        return new CnpjaOfficeResponse(CNPJ, "AMBEV FILIAL MANAUS", false,
                new CnpjaOfficeResponse.Company("AMBEV S.A.",
                        new CnpjaOfficeResponse.SimplesSimei(false),
                        new CnpjaOfficeResponse.SimplesSimei(false)),
                new CnpjaOfficeResponse.Address("Avenida Buriti", "4000", "Galpao 2",
                        "Distrito Industrial", "Manaus", "AM", "69075000"),
                List.of(new CnpjaOfficeResponse.Phone("LANDLINE", "92", "21238000")),
                List.of(new CnpjaOfficeResponse.Email("ACCOUNTING", "contabilidade@ambev.com.br")),
                new CnpjaOfficeResponse.Status(2, "Ativa"),
                List.of(new CnpjaOfficeResponse.Registration("054591406", "AM", true,
                        new CnpjaOfficeResponse.Registration.Type(1, "IE Normal"))));
    }

    @Test
    @DisplayName("put grava o JSON do provedor com o TTL configurado")
    void shouldStoreProviderPayloadWithConfiguredTtl() {
        cnpjaCache.put(CNPJ, office());

        ArgumentCaptor<CachedOffice> captor = ArgumentCaptor.forClass(CachedOffice.class);
        verify(repository).save(captor.capture());

        CachedOffice salvo = captor.getValue();
        assertThat(salvo.getCnpj()).isEqualTo(CNPJ);
        assertThat(salvo.getTtlInSeconds()).isEqualTo(Duration.ofHours(24).toSeconds());
        assertThat(salvo.getPayload()).contains("AMBEV S.A.").contains("054591406");
    }

    @Test
    @DisplayName("get devolve o objeto desserializado, com os blocos aninhados intactos")
    void shouldReturnDeserializedOffice() throws Exception {
        String payload = objectMapper.writeValueAsString(office());
        when(repository.findById(CNPJ)).thenReturn(Optional.of(new CachedOffice(CNPJ, payload, 86400L)));

        CnpjaOfficeResponse office = cnpjaCache.get(CNPJ).orElseThrow();

        // O round-trip do Jackson sobre records aninhados é justamente o que o
        // mapeamento automático do Redis não deixaria verificar sem um Redis real.
        assertThat(office.taxId()).isEqualTo(CNPJ);
        assertThat(office.company().name()).isEqualTo("AMBEV S.A.");
        assertThat(office.address().city()).isEqualTo("Manaus");
        assertThat(office.phones().get(0).area()).isEqualTo("92");
        assertThat(office.emails().get(0).address()).isEqualTo("contabilidade@ambev.com.br");
        assertThat(office.status().text()).isEqualTo("Ativa");
        assertThat(office.registrations().get(0).number()).isEqualTo("054591406");
        assertThat(office.registrations().get(0).type().id()).isEqualTo(1);
    }

    @Test
    @DisplayName("Sem entrada no cache -> vazio")
    void shouldReturnEmptyOnMiss() {
        when(repository.findById(CNPJ)).thenReturn(Optional.empty());

        assertThat(cnpjaCache.get(CNPJ)).isEmpty();
    }

    @Test
    @DisplayName("Entrada corrompida é tratada como miss, não como erro")
    void shouldTreatCorruptEntryAsMiss() {
        // Payload de um formato antigo, ou truncado. Estourar aqui transformaria
        // uma linha velha no Redis em falha da consulta.
        when(repository.findById(CNPJ))
                .thenReturn(Optional.of(new CachedOffice(CNPJ, "{isso não é json", 86400L)));

        assertThat(cnpjaCache.get(CNPJ)).isEmpty();
    }

    @Test
    @DisplayName("Redis fora do ar na leitura -> miss, a consulta segue para a CNPJá")
    void shouldDegradeGracefullyWhenRedisIsDownOnRead() {
        when(repository.findById(anyString())).thenThrow(new QueryTimeoutException("Redis fora"));

        assertThat(cnpjaCache.get(CNPJ)).isEmpty();
    }

    @Test
    @DisplayName("Redis fora do ar na escrita não derruba a consulta")
    void shouldDegradeGracefullyWhenRedisIsDownOnWrite() {
        when(repository.save(any())).thenThrow(new QueryTimeoutException("Redis fora"));

        // O cache é otimização: indisponibilidade dele não pode virar
        // indisponibilidade do cadastro.
        assertThatCode(() -> cnpjaCache.put(CNPJ, office())).doesNotThrowAnyException();
    }
}
