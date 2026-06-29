package com.aimedical.modules.commonmodule.api;

import com.aimedical.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthErrorCodeTest {

    @Test
    void shouldDefineFourConstants() {
        assertEquals(4, AuthErrorCode.values().length);
        assertNotNull(AuthErrorCode.valueOf("AUTH_MOBILE_EXISTS"));
        assertNotNull(AuthErrorCode.valueOf("AUTH_LOGIN_FAILED"));
        assertNotNull(AuthErrorCode.valueOf("AUTH_ACCOUNT_DISABLED"));
        assertNotNull(AuthErrorCode.valueOf("AUTH_TOKEN_INVALID"));
    }

    @Test
    void shouldReturnCodeForMobileExists() {
        assertEquals("AUTH_MOBILE_EXISTS", AuthErrorCode.AUTH_MOBILE_EXISTS.getCode());
    }

    @Test
    void shouldReturnMessageForMobileExists() {
        assertEquals("该手机号已注册", AuthErrorCode.AUTH_MOBILE_EXISTS.getMessage());
    }

    @Test
    void shouldReturnCodeForLoginFailed() {
        assertEquals("AUTH_LOGIN_FAILED", AuthErrorCode.AUTH_LOGIN_FAILED.getCode());
    }

    @Test
    void shouldReturnMessageForLoginFailed() {
        assertEquals("用户名或密码错误", AuthErrorCode.AUTH_LOGIN_FAILED.getMessage());
    }

    @Test
    void shouldReturnCodeForAccountDisabled() {
        assertEquals("AUTH_ACCOUNT_DISABLED", AuthErrorCode.AUTH_ACCOUNT_DISABLED.getCode());
    }

    @Test
    void shouldReturnMessageForAccountDisabled() {
        assertEquals("账户已被禁用", AuthErrorCode.AUTH_ACCOUNT_DISABLED.getMessage());
    }

    @Test
    void shouldReturnCodeForTokenInvalid() {
        assertEquals("AUTH_TOKEN_INVALID", AuthErrorCode.AUTH_TOKEN_INVALID.getCode());
    }

    @Test
    void shouldReturnMessageForTokenInvalid() {
        assertEquals("令牌无效或已过期", AuthErrorCode.AUTH_TOKEN_INVALID.getMessage());
    }

    @Test
    void shouldImplementErrorCode() {
        for (AuthErrorCode code : AuthErrorCode.values()) {
            assertInstanceOf(ErrorCode.class, code);
        }
    }
}
