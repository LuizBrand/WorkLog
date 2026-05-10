package br.com.luizbrand.worklog.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("worklog.cookies")
public record CookieProperties(
        @DefaultValue("false") boolean secure,
        @DefaultValue("Strict") String sameSite,
        @DefaultValue("worklog_access") String accessName,
        @DefaultValue("worklog_refresh") String refreshName,
        @DefaultValue("/worklog/auth") String refreshPath) {
}
