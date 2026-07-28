package br.com.luizbrand.worklog.client.integration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Monta o {@link RestClient} da CNPJá.
 *
 * <p>Usa {@code RestClient.builder()} estático em vez do builder autoconfigurado
 * do Boot: assim esta classe não depende de nenhum bean de autoconfiguração e
 * continua carregando dentro de fatias {@code @WebMvcTest}.
 *
 * <p>Timeout explícito é obrigatório aqui — o default do JDK é <i>infinito</i>,
 * e uma consulta pendurada seguraria a thread da requisição do usuário.
 */
@Configuration
@EnableConfigurationProperties(CnpjaProperties.class)
public class CnpjaConfig {

    @Bean
    public RestClient cnpjaRestClient(CnpjaProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.connectTimeout().toMillis());
        requestFactory.setReadTimeout((int) properties.readTimeout().toMillis());

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
