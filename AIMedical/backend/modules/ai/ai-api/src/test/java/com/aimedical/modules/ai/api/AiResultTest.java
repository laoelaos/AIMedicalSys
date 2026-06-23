package com.aimedical.modules.ai.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiResultTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        AiResult<String> result = new AiResult<>();
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertNull(result.getErrorCode());
        assertFalse(result.isDegraded());
        assertNull(result.getFallbackReason());
    }

    @Test
    void shouldCreateWithAllArgsConstructor() {
        AiResult<Integer> result = new AiResult<>(true, 42, null, false, null);
        assertTrue(result.isSuccess());
        assertEquals(42, result.getData());
        assertNull(result.getErrorCode());
        assertFalse(result.isDegraded());
        assertNull(result.getFallbackReason());
    }

    @Test
    void shouldCreateSuccessResultViaFactory() {
        AiResult<String> result = AiResult.success("ok");
        assertTrue(result.isSuccess());
        assertEquals("ok", result.getData());
        assertNull(result.getErrorCode());
        assertFalse(result.isDegraded());
        assertNull(result.getFallbackReason());
    }

    @Test
    void shouldCreateSuccessResultWithNullData() {
        AiResult<String> result = AiResult.success(null);
        assertTrue(result.isSuccess());
        assertNull(result.getData());
    }

    @Test
    void shouldCreateFailureResultViaFactory() {
        AiResult<Void> result = AiResult.failure("ERR_TIMEOUT");
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertEquals("ERR_TIMEOUT", result.getErrorCode());
        assertFalse(result.isDegraded());
        assertNull(result.getFallbackReason());
    }

    @Test
    void shouldCreateDegradedResultViaFactory() {
        AiResult<Void> result = AiResult.degraded("fallback to cache");
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertNull(result.getErrorCode());
        assertTrue(result.isDegraded());
        assertEquals("fallback to cache", result.getFallbackReason());
    }

    @Test
    void shouldSetAndGetSuccess() {
        AiResult<String> result = new AiResult<>();
        result.setSuccess(true);
        assertTrue(result.isSuccess());
        result.setSuccess(false);
        assertFalse(result.isSuccess());
    }

    @Test
    void shouldSetAndGetData() {
        AiResult<String> result = new AiResult<>();
        result.setData("hello");
        assertEquals("hello", result.getData());
        result.setData(null);
        assertNull(result.getData());
    }

    @Test
    void shouldSetAndGetErrorCode() {
        AiResult<String> result = new AiResult<>();
        result.setErrorCode("ERR_001");
        assertEquals("ERR_001", result.getErrorCode());
    }

    @Test
    void shouldSetAndGetDegraded() {
        AiResult<String> result = new AiResult<>();
        result.setDegraded(true);
        assertTrue(result.isDegraded());
    }

    @Test
    void shouldSetAndGetFallbackReason() {
        AiResult<String> result = new AiResult<>();
        result.setFallbackReason("reason");
        assertEquals("reason", result.getFallbackReason());
    }

    @Test
    void shouldSupportDifferentGenericTypes() {
        AiResult<Integer> intResult = AiResult.success(100);
        assertEquals(100, intResult.getData());

        AiResult<Double> doubleResult = AiResult.success(3.14);
        assertEquals(3.14, doubleResult.getData(), 0.001);
    }
}
