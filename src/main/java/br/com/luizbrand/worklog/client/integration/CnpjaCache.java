package br.com.luizbrand.worklog.client.integration;

import br.com.luizbrand.worklog.client.integration.dto.CnpjaOfficeResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Cache das consultas de CNPJ no Redis, com TTL.
 *
 * <p>Existe por causa do limite de 5 consultas por minuto por IP: cadastrar duas
 * filiais do mesmo grupo, ou reabrir um formulário depois de errar um campo,
 * gastava uma consulta cada vez.
 *
 * <p><b>Toda falha do cache é engolida de propósito.</b> Redis fora do ar,
 * entrada de um formato antigo ou payload truncado viram "não tem no cache", e
 * a consulta segue para a CNPJá. Cache é otimização — indisponibilidade dele
 * não pode virar indisponibilidade do cadastro.
 */
@Component
public class CnpjaCache {

    private static final Logger logger = LoggerFactory.getLogger(CnpjaCache.class);

    private final CachedOfficeRepository repository;
    private final CnpjaProperties properties;
    private final ObjectMapper objectMapper;

    public CnpjaCache(CachedOfficeRepository repository, CnpjaProperties properties, ObjectMapper objectMapper) {
        this.repository = repository;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public Optional<CnpjaOfficeResponse> get(String cnpj) {
        try {
            return repository.findById(cnpj)
                    .map(CachedOffice::getPayload)
                    .map(this::desserializar);
        } catch (DataAccessException e) {
            logger.warn("Falha ao ler o cache de CNPJ; seguindo para o provedor", e);
            return Optional.empty();
        }
    }

    public void put(String cnpj, CnpjaOfficeResponse office) {
        try {
            repository.save(new CachedOffice(
                    cnpj,
                    objectMapper.writeValueAsString(office),
                    properties.cacheTtl().toSeconds()));
        } catch (JsonProcessingException | DataAccessException e) {
            logger.warn("Não foi possível cachear a consulta do CNPJ {}", cnpj, e);
        }
    }

    /**
     * Devolve {@code null} — e não uma exceção — para que o {@code Optional.map}
     * acima transforme entrada ilegível em ausência.
     */
    private CnpjaOfficeResponse desserializar(String payload) {
        try {
            return objectMapper.readValue(payload, CnpjaOfficeResponse.class);
        } catch (JsonProcessingException e) {
            logger.warn("Entrada de cache ilegível, tratada como ausente", e);
            return null;
        }
    }
}
