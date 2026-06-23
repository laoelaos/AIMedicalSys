package com.aimedical.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlobalErrorCodeTest {

    @Test
    void shouldHaveExpectedConstants() {
        assertEquals(4, GlobalErrorCode.values().length);
        assertNotNull(GlobalErrorCode.valueOf("SUCCESS"));
        assertNotNull(GlobalErrorCode.valueOf("SYSTEM_ERROR"));
        assertNotNull(GlobalErrorCode.valueOf("PARAM_INVALID"));
        assertNotNull(GlobalErrorCode.valueOf("NOT_FOUND"));
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
