package com.aimedical.modules.admin.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TokenStoreTest {

    @Test
    void shouldHandleAllFieldsViaGettersAndSetters() {
        TokenStore entity = new TokenStore();
        LocalDateTime expiresAt = LocalDateTime.of(2025, 12, 31, 23, 59, 59);

        entity.setUserId(1001L);
        entity.setToken("access-token-abc");
        entity.setRefreshToken("refresh-token-xyz");
        entity.setTokenType("Bearer");
        entity.setExpiresAt(expiresAt);

        assertEquals(1001L, entity.getUserId());
        assertEquals("access-token-abc", entity.getToken());
        assertEquals("refresh-token-xyz", entity.getRefreshToken());
        assertEquals("Bearer", entity.getTokenType());
        assertEquals(expiresAt, entity.getExpiresAt());
    }

    @Test
    void shouldSupportEqualsAcrossAllBranches() {
        TokenStore a = new TokenStore();
        a.setToken("t1");
        a.setUserId(1L);

        TokenStore same = new TokenStore();
        same.setToken("t1");
        same.setUserId(1L);

        TokenStore different = new TokenStore();
        different.setToken("t2");
        different.setUserId(2L);

        assertTrue(a.equals(a));
        assertTrue(a.equals(same));
        assertEquals(a.hashCode(), same.hashCode());
        assertFalse(a.equals(different));
        assertFalse(a.equals(null));
        assertFalse(a.equals("not a TokenStore"));
    }

    @Test
    void shouldGenerateNonEmptyToString() {
        TokenStore entity = new TokenStore();
        entity.setUserId(1001L);
        entity.setToken("my-token");
        entity.setTokenType("Bearer");
        // toString 应包含非敏感字段
        assertTrue(entity.toString().contains("1001"));
        assertTrue(entity.toString().contains("Bearer"));
        // toString 不应泄露敏感 token（安全要求：@ToString 排除 token/refreshToken）
        assertFalse(entity.toString().contains("my-token"));
    }

    @Test
    void shouldInvokeHashCodeWithAllFieldsNull() {
        TokenStore entity = new TokenStore();
        entity.hashCode();
    }

    @Test
    void shouldInvokeHashCodeWithAllFieldsSet() {
        TokenStore entity = new TokenStore();
        entity.setUserId(1L);
        entity.setToken("t");
        entity.setRefreshToken("r");
        entity.setTokenType("Bearer");
        entity.setExpiresAt(LocalDateTime.now());
        entity.hashCode();
    }
}