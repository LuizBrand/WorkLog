package br.com.luizbrand.worklog.client.integration;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.util.concurrent.TimeUnit;

/**
 * Resposta da CNPJá guardada no Redis, indexada pelo CNPJ normalizado.
 *
 * <p>O payload é o <b>JSON cru do provedor</b>, não o nosso DTO de resposta: o
 * mapeamento é reaplicado a cada consulta, então mudar uma regra de tradução
 * passa a valer na hora, sem esperar o cache expirar.
 *
 * <p>Guardado como {@code String} em vez de deixar o Spring Data mapear o
 * objeto: a resposta tem listas de objetos aninhados, e o round-trip do Jackson
 * é verificável em teste unitário — o mapeamento do Redis só quebraria contra
 * um Redis de verdade.
 */
@RedisHash("cnpja_offices")
public class CachedOffice {

    @Id
    private String cnpj;

    private String payload;

    @TimeToLive(unit = TimeUnit.SECONDS)
    private Long ttlInSeconds;

    public CachedOffice() {
    }

    public CachedOffice(String cnpj, String payload, Long ttlInSeconds) {
        this.cnpj = cnpj;
        this.payload = payload;
        this.ttlInSeconds = ttlInSeconds;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Long getTtlInSeconds() {
        return ttlInSeconds;
    }

    public void setTtlInSeconds(Long ttlInSeconds) {
        this.ttlInSeconds = ttlInSeconds;
    }
}
