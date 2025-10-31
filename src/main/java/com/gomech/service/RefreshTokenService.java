package com.gomech.service;

import com.gomech.model.RefreshToken;
import com.gomech.model.User;
import com.gomech.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class RefreshTokenService {

    private final SecureRandom secureRandom = new SecureRandom();
    private final RefreshTokenRepository refreshTokenRepository;
    private final EncryptionService encryptionService;
    private final Duration refreshTokenTtl;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               EncryptionService encryptionService,
                               @Value("${security.refresh-token.ttl-hours:168}") long ttlHours) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.encryptionService = encryptionService;
        this.refreshTokenTtl = Duration.ofHours(ttlHours);
    }

    @Transactional
    public String createToken(User user) {
        String rawToken = generateRawToken();
        String encryptedToken = encryptionService.encrypt(rawToken);
        String tokenHash = encryptionService.sha256(rawToken);
        RefreshToken refreshToken = new RefreshToken(user, encryptedToken, tokenHash, Instant.now().plus(refreshTokenTtl));
        refreshTokenRepository.save(refreshToken);
        user.getRefreshTokens().add(refreshToken);
        return rawToken;
    }

    @Transactional
    public Optional<RefreshToken> validate(String rawToken) {
        String tokenHash = encryptionService.sha256(rawToken);
        Optional<RefreshToken> storedToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash);
        if (storedToken.isEmpty()) {
            return Optional.empty();
        }
        RefreshToken refreshToken = storedToken.get();
        if (refreshToken.isExpired()) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            return Optional.empty();
        }
        return Optional.of(refreshToken);
    }

    @Transactional
    public void revoke(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
