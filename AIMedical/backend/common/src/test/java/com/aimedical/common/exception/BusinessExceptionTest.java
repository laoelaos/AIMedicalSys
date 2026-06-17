package com.aimedical.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    private static final ErrorCode TEST_ERROR = new ErrorCode() {
        @Override
        public String code() {
            return "TEST_ERR";
        }

        @Override
        public String message() {
            return "测试异常";
        }
    };

    @Test
    void shouldCreateWithErrorCode() {
        BusinessException ex = new BusinessException(TEST_ERROR);
        assertEquals(TEST_ERROR, ex.getErrorCode());
        assertEquals("测试异常", ex.getMessage());
        assertNull(ex.getArgs());
        assertNull(ex.getCause());
    }

    @Test
    void shouldCreateWithErrorCodeAndArgs() {
        BusinessException ex = new BusinessException(TEST_ERROR, "arg1", 42);
        assertEquals(TEST_ERROR, ex.getErrorCode());
        assertArrayEquals(new Object[]{"arg1", 42}, ex.getArgs());
    }

    @Test
    void shouldCreateWithErrorCodeAndCause() {
        Throwable cause = new RuntimeException("root cause");
        BusinessException ex = new BusinessException(TEST_ERROR, cause);
        assertEquals(TEST_ERROR, ex.getErrorCode());
        assertSame(cause, ex.getCause());
        assertNull(ex.getArgs());
    }

    @Test
    void shouldBeInstanceOfRuntimeException() {
        BusinessException ex = new BusinessException(TEST_ERROR);
        assertInstanceOf(RuntimeException.class, ex);
    }
}
