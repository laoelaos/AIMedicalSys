package com.aimedical.config;

import com.aimedical.modules.commonmodule.api.TokenProvider;
import com.aimedical.modules.commonmodule.api.dto.CurrentUserResponse;
import com.aimedical.modules.commonmodule.api.dto.TokenResponse;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component("applicationJwtTokenProvider")
public class JwtTokenProvider implements TokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey accessTokenKey;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    /**
     * In-memory token blacklist for logout.
     * In production, replace with Redis or a database table.
     */
    private final Map<String, Long> blacklistedTokens = new ConcurrentHashMap<>();
    private final Map<String, String> refreshTokenFamily = new ConcurrentHashMap<>();

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.refresh-secret:#{null}}") String refreshSecret,
            @Value("${jwt.access-token-validity:7200}") long accessTokenValidity,
            @Value("${jwt.refresh-token-validity:604800}") long refreshTokenValidity) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length * 8 < 256) {
            log.warn("JWT secret is less than 256 bits ({} bits). Consider using a stronger key.", secretBytes.length * 8);
        }
        this.accessTokenKey = Keys.hmacShaKeyFor(secretBytes);
        // Use a derived key for refresh tokens if no separate secret provided
        String actualRefreshSecret = (refreshSecret != null && !refreshSecret.isBlank()) ? refreshSecret : secret + ":refresh";
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
    }

    @Override
    public TokenResponse generateTokens(Long userId, String username) {
        String accessToken = generateAccessToken(userId, username);
        String refreshToken = generateRefreshToken(userId, username);
        String familyId = UUID.randomUUID().toString();
        refreshTokenFamily.put(refreshToken, familyId);
        return new TokenResponse(accessToken, refreshToken, accessTokenValidity);
    }

    @Override
    public String generateAccessToken(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenValidity * 1000);
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(accessTokenKey)
                .compact();
    }

    @Override
    public String generateRefreshToken(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenValidity * 1000);
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(accessTokenKey)
                .compact();
    }

    @Override
    public boolean validateToken(String token) {
        return validateToken(token, "access");
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token, "refresh");
    }

    private boolean validateToken(String token, String expectedType) {
        if (blacklistedTokens.containsKey(token)) {
            return false;
        }
        try {
            Claims claims = parseClaims(token);
            String type = claims.get("type", String.class);
            if (!expectedType.equals(type)) {
                log.debug("Token type mismatch: expected={}, actual={}", expectedType, type);
                return false;
            }
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getUsernameFromToken(String token) {
        if (blacklistedTokens.containsKey(token)) {
            return null;
        }
        return parseClaims(token).getSubject();
    }

    @Override
    public CurrentUserResponse getCurrentUserFromToken(String token) {
        if (blacklistedTokens.containsKey(token)) {
            return null;
        }
        Claims claims = parseClaims(token);
        CurrentUserResponse user = new CurrentUserResponse();
        user.setUserId(claims.get("userId", Long.class));
        user.setUsername(claims.getSubject());
        return user;
    }

    @Override
    public void invalidateToken(String token) {
        blacklistedTokens.put(token, System.currentTimeMillis() + accessTokenValidity * 1000);
    }

    public void invalidateRefreshTokenFamily(String refreshToken) {
        String familyId = refreshTokenFamily.remove(refreshToken);
        if (familyId != null) {
            refreshTokenFamily.entrySet().removeIf(e -> familyId.equals(e.getValue()));
        }
        blacklistedTokens.put(refreshToken, System.currentTimeMillis() + refreshTokenValidity * 1000);
    }

    public long getRefreshTokenValidity() {
        return refreshTokenValidity;
    }

    public void cleanupExpiredBlacklist() {
        long now = System.currentTimeMillis();
        blacklistedTokens.entrySet().removeIf(e -> e.getValue() < now);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(accessTokenKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
