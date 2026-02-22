package br.com.luizbrand.worklog.auth.refreshtoken;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RedisHash("refresh_tokens")
public class RefreshToken {

    @Id
    private String id;

    @Indexed
    private String userEmail;

    @TimeToLive(unit = TimeUnit.MILLISECONDS)
    private Long expirationInMillis;

    public RefreshToken() {
    }

    public RefreshToken(String userEmail, Long expirationInMillis) {
        this.id = UUID.randomUUID().toString();
        this.userEmail = userEmail;
        this.expirationInMillis = expirationInMillis;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public Long getExpirationInMillis() {
        return expirationInMillis;
    }

    public void setExpirationInMillis(Long expirationInMillis) {
        this.expirationInMillis = expirationInMillis;
    }
}
