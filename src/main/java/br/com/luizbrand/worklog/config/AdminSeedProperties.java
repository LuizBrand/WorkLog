package br.com.luizbrand.worklog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Bootstrap do primeiro usuário ADMIN. Quando {@code email} e {@code password}
 * estão definidos (via env {@code ADMIN_EMAIL} / {@code ADMIN_PASSWORD}), o
 * {@link AdminSeeder} cria o admin na primeira subida se ele ainda não existir.
 * Vazios (dev/test) = seed desligado.
 */
@ConfigurationProperties("worklog.admin")
public record AdminSeedProperties(
        @DefaultValue("") String email,
        @DefaultValue("") String password,
        @DefaultValue("Admin") String name) {
}
