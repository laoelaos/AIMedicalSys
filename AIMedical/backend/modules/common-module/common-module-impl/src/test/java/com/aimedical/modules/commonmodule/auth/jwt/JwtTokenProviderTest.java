package com.aimedical.modules.commonmodule.auth.jwt;

import com.aimedical.modules.commonmodule.jwt.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private static final String TEST_SECRET = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    private JwtConfig jwtConfig;
    private JwtTokenProvider jwtTokenProvider;
    private SecretKey testKey;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        jwtConfig.setSecret(TEST_SECRET);
        jwtTokenProvider = new JwtTokenProvider(jwtConfig);
        jwtTokenProvider.init();
        byte[] keyBytes = Base64.getDecoder().decode(TEST_SECRET);
        testKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes);
    }

    @Test
    void generateAccessToken_shouldCreateValidToken() {
        String token = jwtTokenProvider.generateAccessToken(1L, "testuser", "DOCTOR", "test-jti");

        assertNotNull(token);
        Claims claims = jwtTokenProvider.validateToken(token, null);
        assertNotNull(claims);
        assertEquals("testuser", claims.getSubject());
        assertEquals(1L, jwtTokenProvider.getUserIdFromClaims(claims));
        assertEquals("DOCTOR", claims.get("userType"));
        assertEquals("test-jti", claims.get("jti"));
        assertEquals("access", claims.get("type"));

        long diff = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertEquals(900_000L, diff);
    }

    @Test
    void generateRefreshToken_shouldContainTypeAndVersion() {
        String token = jwtTokenProvider.generateRefreshToken(1L, "testuser", "DOCTOR", 0, "test-jti");

        Claims claims = jwtTokenProvider.validateToken(token, null);
        assertNotNull(claims);
        assertEquals("refresh", claims.get("type"));
        assertEquals(0, jwtTokenProvider.getTokenVersionFromClaims(claims));
    }

    @Test
    void validateToken_withCorrectType_shouldReturnClaims() {
        String token = jwtTokenProvider.generateAccessToken(1L, "testuser", "DOCTOR");

        Claims claims = jwtTokenProvider.validateToken(token, null);

        assertNotNull(claims);
        assertNotNull(jwtTokenProvider.validateToken(token, "access"));
    }

    @Test
    void validateToken_withWrongType_shouldReturnNull() {
        String token = jwtTokenProvider.generateRefreshToken(1L, "testuser", "DOCTOR", 0);

        Claims claims = jwtTokenProvider.validateToken(token, "access");

        assertNull(claims);
    }

    @Test
    void validateToken_withExpiredToken_shouldReturnNull() {
        long past = System.currentTimeMillis() - 10_000L;
        String expiredToken = Jwts.builder()
                .subject("testuser")
                .claim("userId", 1L)
                .claim("userType", "DOCTOR")
                .issuedAt(new Date(past - 60_000L))
                .expiration(new Date(past))
                .signWith(testKey)
                .compact();

        Claims claims = jwtTokenProvider.validateToken(expiredToken, null);

        assertNull(claims);
    }

    @Test
    void getUserIdFromClaims_withInteger_shouldReturnLong() {
        Claims claims = Jwts.claims()
                .add("userId", 1)
                .build();

        Long result = jwtTokenProvider.getUserIdFromClaims(claims);

        assertEquals(1L, result);
    }

    @Test
    void getUserIdFromClaims_withLong_shouldReturnLong() {
        Claims claims = Jwts.claims()
                .add("userId", 1L)
                .build();

        Long result = jwtTokenProvider.getUserIdFromClaims(claims);

        assertEquals(1L, result);
    }

    @Test
    void getJtiFromToken_shouldReturnCorrectJti() {
        String token = jwtTokenProvider.generateAccessToken(1L, "testuser", "DOCTOR", "expected-jti");

        String jti = jwtTokenProvider.getJtiFromToken(token);

        assertEquals("expected-jti", jti);
    }

    @Test
    void getTokenType_shouldReturnValueFromJwtConfig() {
        String tokenType = jwtTokenProvider.getTokenType();

        assertEquals("Bearer", tokenType);
    }

    @Test
    void getTokenType_shouldReflectChangesToJwtConfig() {
        jwtConfig.setTokenType("CustomType");
        String tokenType = jwtTokenProvider.getTokenType();

        assertEquals("CustomType", tokenType);
    }

    @Test
    void shouldThrowWhenSecretIsNull() {
        JwtConfig config = new JwtConfig();
        config.setSecret(null);
        JwtTokenProvider provider = new JwtTokenProvider(config);
        IllegalStateException ex = assertThrows(IllegalStateException.class, provider::init);
        assertTrue(ex.getMessage().contains("JWT secret must be configured"));
    }

    @Test
    void shouldThrowWhenSecretIsEmpty() {
        JwtConfig config = new JwtConfig();
        config.setSecret("");
        JwtTokenProvider provider = new JwtTokenProvider(config);
        IllegalStateException ex = assertThrows(IllegalStateException.class, provider::init);
        assertTrue(ex.getMessage().contains("JWT secret must be configured"));
    }

    @Test
    void shouldThrowWhenSecretContainsInvalidChars() {
        JwtConfig config = new JwtConfig();
        config.setSecret("test-secret!!!");
        JwtTokenProvider provider = new JwtTokenProvider(config);
        IllegalStateException ex = assertThrows(IllegalStateException.class, provider::init);
        assertTrue(ex.getMessage().contains("URL-safe"));
    }

    @Test
    void shouldThrowWhenBase64DecodeFailsAfterRegexPass() {
        JwtConfig config = new JwtConfig();
        config.setSecret("abcde");
        JwtTokenProvider provider = new JwtTokenProvider(config);
        IllegalStateException ex = assertThrows(IllegalStateException.class, provider::init);
        assertTrue(ex.getMessage().contains("not a valid Base64 string"));
    }

    @Test
    void shouldThrowWhenDecodedKeyTooShort() {
        JwtConfig config = new JwtConfig();
        config.setSecret("dGVzdA");
        JwtTokenProvider provider = new JwtTokenProvider(config);
        IllegalStateException ex = assertThrows(IllegalStateException.class, provider::init);
        assertTrue(ex.getMessage().contains("at least 32 bytes"));
    }

    @Test
    void shouldInitSuccessfullyWithValidSecret() {
        JwtConfig config = new JwtConfig();
        config.setSecret("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        JwtTokenProvider provider = new JwtTokenProvider(config);
        assertDoesNotThrow(provider::init);
    }
}
