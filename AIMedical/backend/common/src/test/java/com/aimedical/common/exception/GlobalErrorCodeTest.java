package com.aimedical.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlobalErrorCodeTest {

    @Test
    void shouldHaveExpectedConstants() {
        assertEquals(20, GlobalErrorCode.values().length);
        assertNotNull(GlobalErrorCode.valueOf("SUCCESS"));
        assertNotNull(GlobalErrorCode.valueOf("SYSTEM_ERROR"));
        assertNotNull(GlobalErrorCode.valueOf("PARAM_INVALID"));
        assertNotNull(GlobalErrorCode.valueOf("NOT_FOUND"));
        assertNotNull(GlobalErrorCode.valueOf("UNAUTHORIZED"));
        assertNotNull(GlobalErrorCode.valueOf("FORBIDDEN"));
        assertNotNull(GlobalErrorCode.valueOf("LOGIN_FAILED"));
        assertNotNull(GlobalErrorCode.valueOf("ACCOUNT_DISABLED"));
        assertNotNull(GlobalErrorCode.valueOf("ACCOUNT_LOCKED"));
        assertNotNull(GlobalErrorCode.valueOf("RATE_LIMITED"));
        assertNotNull(GlobalErrorCode.valueOf("RATE_LIMITED_GLOBAL"));
        assertNotNull(GlobalErrorCode.valueOf("PASSWORD_TOO_SHORT"));
        assertNotNull(GlobalErrorCode.valueOf("PASSWORD_TOO_LONG"));
        assertNotNull(GlobalErrorCode.valueOf("PASSWORD_WEAK"));
        assertNotNull(GlobalErrorCode.valueOf("PASSWORD_CONTAINS_USERNAME"));
        assertNotNull(GlobalErrorCode.valueOf("PASSWORD_COMMON"));
        assertNotNull(GlobalErrorCode.valueOf("TOKEN_REFRESH_FAILED"));
        assertNotNull(GlobalErrorCode.valueOf("PASSWORD_CHANGE_REQUIRED"));
        assertNotNull(GlobalErrorCode.valueOf("CHILDREN_EXIST"));
        assertNotNull(GlobalErrorCode.valueOf("PASSWORD_MISMATCH"));
    }

    @Test
    void successShouldReturnCorrectCodeAndMessage() {
        assertEquals("SUCCESS", GlobalErrorCode.SUCCESS.getCode());
        assertEquals("成功", GlobalErrorCode.SUCCESS.getMessage());
    }

    @Test
    void systemErrorShouldReturnCorrectCodeAndMessage() {
        assertEquals("SYSTEM_ERROR", GlobalErrorCode.SYSTEM_ERROR.getCode());
        assertEquals("系统异常", GlobalErrorCode.SYSTEM_ERROR.getMessage());
    }

    @Test
    void paramInvalidShouldReturnCorrectCodeAndMessage() {
        assertEquals("PARAM_INVALID", GlobalErrorCode.PARAM_INVALID.getCode());
        assertEquals("参数校验失败", GlobalErrorCode.PARAM_INVALID.getMessage());
    }

    @Test
    void notFoundShouldReturnCorrectCodeAndMessage() {
        assertEquals("NOT_FOUND", GlobalErrorCode.NOT_FOUND.getCode());
        assertEquals("资源不存在", GlobalErrorCode.NOT_FOUND.getMessage());
    }
}
