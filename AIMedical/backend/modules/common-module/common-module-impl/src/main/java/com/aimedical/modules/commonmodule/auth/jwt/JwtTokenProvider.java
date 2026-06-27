package com.aimedical.modules.commonmodule.auth.jwt;

import com.aimedical.modules.commonmodule.jwt.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final long ACCESS_TOKEN_EXPIRATION_MS = 900_000L;
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 604_800_000L;

    private final JwtConfig jwtConfig;
    private SecretKey secretKey;

    public JwtTokenProvider(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @PostConstruct
    public void init() {
        String secret = jwtConfig.getSecret();
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("JWT secret must be configured");
        }
        if (!secret.matches("^[A-Za-z0-9_\\-=]+$")) {
            throw new IllegalStateException("JWT secret contains invalid URL-safe Base64 characters");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getUrlDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT secret is not a valid Base64 string: " + e.getMessage());
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must decode to at least 32 bytes (256 bits), got: " + keyBytes.length);
        }
        this.secretKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Long userId, String username, String userType, String jti) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("userType", userType)
                .claim("jti", jti)
                .claim("type", "access")
                .issuedAt(new Date(now))
                .expiration(new Date(now + ACCESS_TOKEN_EXPIRATION_MS))
                .signWith(secretKey)
                .compact();
    }

    public String generateAccessToken(Long userId, String username, String userType) {
        return generateAccessToken(userId, username, userType, UUID.randomUUID().toString());
    }

    public String generateRefreshToken(Long userId, String username, String userType, int tokenVersion, String jti) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("userType", userType)
                .claim("type", "refresh")
                .claim("tokenVersion", tokenVersion)
                .claim("jti", jti)
                .issuedAt(new Date(now))
                .expiration(new Date(now + REFRESH_TOKEN_EXPIRATION_MS))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(Long userId, String username, String userType, int tokenVersion) {
        return generateRefreshToken(userId, username, userType, tokenVersion, UUID.randomUUID().toString());
    }

    public Claims validateToken(String token, String expectedType) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            Claims claims = jws.getPayload();
            if (expectedType != null) {
                String type = claims.get("type", String.class);
                if (!expectedType.equals(type)) {
                    return null;
                }
            }
            return claims;
        } catch (ExpiredJwtException | SignatureException | MalformedJwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public Long getUserIdFromClaims(Claims claims) {
        Object userId = claims.get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        } else if (userId instanceof Long) {
            return (Long) userId;
        }
        return null;
    }

    public String getJtiFromToken(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return jws.getPayload().get("jti", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public String getTokenType() {
        return jwtConfig.getTokenType();
    }

    public int getTokenVersionFromClaims(Claims claims) {
        Integer version = claims.get("tokenVersion", Integer.class);
        return version != null ? version : 0;
    }

    public long getAccessTokenExpirationMs() {
        return ACCESS_TOKEN_EXPIRATION_MS;
    }
}
