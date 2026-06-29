package com.aimedical.modules.commonmodule.api.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenResponseTest {

    @Test
    void shouldCreateViaNoArgConstructor() {
        TokenResponse response = new TokenResponse();
        assertNotNull(response);
        assertEquals("Bearer", response.getTokenType());
    }

    @Test
    void shouldCreateViaAllArgsConstructor() {
        TokenResponse response = new TokenResponse("access123", "refresh456", 3600L);
        assertEquals("access123", response.getAccessToken());
        assertEquals("refresh456", response.getRefreshToken());
        assertEquals(3600L, response.getExpiresIn());
    }

    @Test
    void shouldSetAndGetAccessToken() {
        TokenResponse response = new TokenResponse();
        response.setAccessToken("newAccess");
        assertEquals("newAccess", response.getAccessToken());
    }

    @Test
    void shouldSetAndGetRefreshToken() {
        TokenResponse response = new TokenResponse();
        response.setRefreshToken("newRefresh");
        assertEquals("newRefresh", response.getRefreshToken());
    }

    @Test
    void shouldSetAndGetTokenType() {
        TokenResponse response = new TokenResponse();
        response.setTokenType("Custom");
        assertEquals("Custom", response.getTokenType());
    }

    @Test
    void shouldSetAndGetExpiresIn() {
        TokenResponse response = new TokenResponse();
        response.setExpiresIn(7200L);
        assertEquals(7200L, response.getExpiresIn());
    }
}
