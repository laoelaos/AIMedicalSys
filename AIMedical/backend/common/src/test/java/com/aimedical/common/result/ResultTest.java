package com.aimedical.common.result;

import com.aimedical.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void shouldCreateSuccessResultWithData() {
        Result<String> result = Result.success("hello");
        assertEquals("SUCCESS", result.getCode());
        assertNull(result.getMessage());
        assertEquals("hello", result.getData());
    }

    @Test
    void shouldCreateSuccessResultWithNullData() {
        Result<String> result = Result.success(null);
        assertEquals("SUCCESS", result.getCode());
        assertNull(result.getData());
    }

    @Test
    void shouldCreateFailResultWithCodeAndMessage() {
        Result<Void> result = Result.fail("ERR_001", "错误信息");
        assertEquals("ERR_001", result.getCode());
        assertEquals("错误信息", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void shouldCreateFailResultWithErrorCode() {
        ErrorCode errorCode = new ErrorCode() {
            @Override
            public String code() {
                return "TEST_ERR";
            }

            @Override
            public String message() {
                return "测试错误";
            }
        };

        Result<Void> result = Result.fail(errorCode);
        assertEquals("TEST_ERR", result.getCode());
        assertEquals("测试错误", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void shouldSetAndGetViaSetters() {
        Result<Integer> result = new Result<>();
        result.setCode("CODE");
        result.setMessage("msg");
        result.setData(42);
        assertEquals("CODE", result.getCode());
        assertEquals("msg", result.getMessage());
        assertEquals(42, result.getData());
    }
}
