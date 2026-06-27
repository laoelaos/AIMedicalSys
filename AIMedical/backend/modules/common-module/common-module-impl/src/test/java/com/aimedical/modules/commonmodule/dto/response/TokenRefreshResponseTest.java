package com.aimedical.modules.commonmodule.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TokenRefreshResponse")
class TokenRefreshResponseTest {

    @Test
    @DisplayName("构造并访问所有字段")
    void shouldConstructAndAccessFields() {
        var response = new TokenRefreshResponse("new-access-token", "new-refresh-token", "Bearer", 900L);

        assertEquals("new-access-token", response.accessToken());
        assertEquals("new-refresh-token", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresIn());
    }

    @Test
    @DisplayName("expiresIn 为原始 long 类型")
    void shouldHavePrimitiveLongExpiresIn() {
        var response = new TokenRefreshResponse("t", null, "Bearer", 86400L);
        assertEquals(86400L, response.expiresIn());
    }

    @Test
    @DisplayName("refreshToken 可为 null（Phase 1 暂为 null）")
    void shouldAcceptNullRefreshToken() {
        var response = new TokenRefreshResponse("t", null, "Bearer", 900L);
        assertNull(response.refreshToken());
    }
}
