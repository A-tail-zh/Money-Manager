package in.atail.moneymanager.service;

import in.atail.moneymanager.entity.ProfileEntity;
import in.atail.moneymanager.entity.RefreshTokenEntity;
import in.atail.moneymanager.exception.ResourceNotFoundException;
import in.atail.moneymanager.exception.UnauthorizedException;
import in.atail.moneymanager.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Transactional
    public RefreshTokenEntity createRefreshToken(ProfileEntity profile) {
        refreshTokenRepository.deleteByProfileId(profile.getId());

        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
                .token(UUID.randomUUID().toString())
                .expiryDate(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                .profile(profile)
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshTokenEntity verifyRefreshToken(String token) {
        RefreshTokenEntity refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token does not exist"));

        if (Boolean.TRUE.equals(refreshToken.getRevoked())) {
            throw new IllegalStateException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new IllegalStateException("Refresh token has expired, please log in again");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
        });
    }

    @Transactional
    public void revokeRefreshToken(String token, Long profileId) {
        RefreshTokenEntity refreshToken = verifyRefreshToken(token);
        if (!refreshToken.getProfile().getId().equals(profileId)) {
            throw new UnauthorizedException("Refresh token does not belong to the current account");
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }
}
