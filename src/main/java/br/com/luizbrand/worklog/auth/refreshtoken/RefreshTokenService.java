package br.com.luizbrand.worklog.auth.refreshtoken;

import br.com.luizbrand.worklog.auth.TokenProperties;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshRepo;
    private final TokenProperties tokenProperties;

    public RefreshTokenService(RefreshTokenRepository refreshRepo, TokenProperties tokenProperties) {
        this.refreshRepo = refreshRepo;
        this.tokenProperties = tokenProperties;
    }

    public RefreshToken generateRefreshToken(String userEmail) {

        Long experationTime = tokenProperties.getRefreshToken().getExpiration();
        RefreshToken refreshToken = new RefreshToken(userEmail, experationTime);
        refreshRepo.save(refreshToken);
        return refreshToken;

    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshRepo.findById(token);
    }

    public void deleteByToken(String token) {
        refreshRepo.deleteById(token);
    }
}
