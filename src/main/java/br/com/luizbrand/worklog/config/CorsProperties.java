package br.com.luizbrand.worklog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

@ConfigurationProperties("worklog.cors")
public record CorsProperties(@DefaultValue List<String> allowedOrigins) {
}
