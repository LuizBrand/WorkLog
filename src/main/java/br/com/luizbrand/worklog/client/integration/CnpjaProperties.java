package br.com.luizbrand.worklog.client.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Configuração da integração com a CNPJá.
 *
 * <p>A API pública não exige cadastro nem chave, então não há segredo a
 * gerenciar. O {@code baseUrl} fica configurável para que a migração para a API
 * comercial, se o limite de 5 consultas/min por IP apertar, seja troca de
 * propriedade e não mudança de código.
 */
@ConfigurationProperties("worklog.cnpja")
public record CnpjaProperties(
        @DefaultValue("https://open.cnpja.com") String baseUrl,
        @DefaultValue("3s") Duration connectTimeout,
        @DefaultValue("10s") Duration readTimeout,
        // TTL curto de propósito: a CNPJá garante defasagem de no máximo 45
        // dias, mas cadastro é operação rara e cache longo esconderia correção
        // recente na Receita.
        @DefaultValue("24h") Duration cacheTtl) {
}
