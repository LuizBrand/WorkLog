package br.com.luizbrand.worklog.support;

import br.com.luizbrand.worklog.auth.refreshtoken.RefreshToken;

import java.util.UUID;

public final class RefreshTokenTestBuilder {

    private String id = UUID.randomUUID().toString();
    private String userEmail = "user@worklog.test";
    private Long expirationInMillis = 604_800_000L;

    public static RefreshTokenTestBuilder aRefreshToken() {
        return new RefreshTokenTestBuilder();
    }

    public RefreshTokenTestBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public RefreshTokenTestBuilder withUserEmail(String userEmail) {
        this.userEmail = userEmail;
        return this;
    }

    public RefreshTokenTestBuilder withExpirationInMillis(Long expirationInMillis) {
        this.expirationInMillis = expirationInMillis;
        return this;
    }

    public RefreshToken build() {
        RefreshToken token = new RefreshToken(userEmail, expirationInMillis);
        token.setId(id);
        return token;
    }
}
