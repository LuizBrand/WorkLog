package br.com.luizbrand.worklog.auth.refreshtoken;

import br.com.luizbrand.worklog.auth.TokenProperties;
import br.com.luizbrand.worklog.support.RefreshTokenTestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshRepo;

    @Mock
    private TokenProperties tokenProperties;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private TokenProperties.RefreshToken refreshProps;

    @BeforeEach
    void setUp() {
        refreshProps = new TokenProperties.RefreshToken();
        refreshProps.setExpiration(604_800_000L);
    }

    @Nested
    @DisplayName("Method: generateRefreshToken()")
    class GenerateRefreshTokenTests {

        @Test
        @DisplayName("Should persist a token bound to the user email with configured TTL")
        void shouldPersistTokenWithTtl() {
            String email = "user@worklog.test";
            when(tokenProperties.getRefreshToken()).thenReturn(refreshProps);

            RefreshToken created = refreshTokenService.generateRefreshToken(email);

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshRepo, times(1)).save(captor.capture());
            RefreshToken saved = captor.getValue();

            assertThat(saved).isSameAs(created);
            assertThat(saved.getUserEmail()).isEqualTo(email);
            assertThat(saved.getExpirationInMillis()).isEqualTo(refreshProps.getExpiration());
            assertThat(saved.getId()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Method: findByToken()")
    class FindByTokenTests {

        @Test
        @DisplayName("Should return token when it exists")
        void shouldReturnTokenWhenPresent() {
            RefreshToken token = RefreshTokenTestBuilder.aRefreshToken().build();
            when(refreshRepo.findById(token.getId())).thenReturn(Optional.of(token));

            Optional<RefreshToken> result = refreshTokenService.findByToken(token.getId());

            assertThat(result).contains(token);
        }

        @Test
        @DisplayName("Should return empty when token is missing")
        void shouldReturnEmptyWhenAbsent() {
            when(refreshRepo.findById("missing")).thenReturn(Optional.empty());

            Optional<RefreshToken> result = refreshTokenService.findByToken("missing");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Method: deleteByToken()")
    class DeleteByTokenTests {

        @Test
        @DisplayName("Should delegate deletion by id to the repository")
        void shouldDelegateDeletion() {
            String tokenId = "token-123";

            refreshTokenService.deleteByToken(tokenId);

            verify(refreshRepo, times(1)).deleteById(tokenId);
        }
    }
}
