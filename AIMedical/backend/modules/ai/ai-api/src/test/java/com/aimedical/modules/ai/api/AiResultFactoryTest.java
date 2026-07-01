package com.aimedical.modules.ai.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiResultFactoryTest {

    @Test
    void shouldCreateFailureResultWithPartialData() {
        AiResult<String> result = AiResultFactory.failure("ERR_TIMEOUT", "partial data");
        assertFalse(result.isSuccess());
        assertEquals("partial data", result.getData());
        assertEquals("ERR_TIMEOUT", result.getErrorCode());
        assertFalse(result.isDegraded());
        assertNull(result.getFallbackReason());
    }

    @Test
    void shouldCreateFailureResultWithNullPartialData() {
        AiResult<String> result = AiResultFactory.failure("ERR_TIMEOUT", null);
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertEquals("ERR_TIMEOUT", result.getErrorCode());
        assertFalse(result.isDegraded());
        assertNull(result.getFallbackReason());
    }

    @Test
    void shouldCreateDegradedResultWithPartialData() {
        AiResult<Integer> result = AiResultFactory.degraded("fallback reason", 42);
        assertFalse(result.isSuccess());
        assertEquals(42, result.getData());
        assertNull(result.getErrorCode());
        assertTrue(result.isDegraded());
        assertEquals("fallback reason", result.getFallbackReason());
    }

    @Test
    void shouldCreateDegradedResultWithNullPartialData() {
        AiResult<String> result = AiResultFactory.degraded("fallback", null);
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertNull(result.getErrorCode());
        assertTrue(result.isDegraded());
        assertEquals("fallback", result.getFallbackReason());
    }

    @Test
    void shouldCreateFailureResultWithoutData() {
        AiResult<Void> result = AiResultFactory.failure("ERR_001");
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertEquals("ERR_001", result.getErrorCode());
        assertFalse(result.isDegraded());
        assertNull(result.getFallbackReason());
    }

    @Test
    void shouldCreateSuccessResultWithData() {
        AiResult<String> result = AiResultFactory.success("ok");
        assertTrue(result.isSuccess());
        assertEquals("ok", result.getData());
        assertNull(result.getErrorCode());
        assertFalse(result.isDegraded());
        assertNull(result.getFallbackReason());
    }

    @Test
    void shouldCreateSuccessResultWithNullData() {
        AiResult<String> result = AiResultFactory.success(null);
        assertTrue(result.isSuccess());
        assertNull(result.getData());
    }

    @Test
    void shouldCreateDegradedResultWithErrorCodeAndPartialData() {
        AiResult<String> result = AiResultFactory.degraded("service unavailable", "ERR_SVC_DOWN", "partial");
        assertFalse(result.isSuccess());
        assertEquals("partial", result.getData());
        assertEquals("ERR_SVC_DOWN", result.getErrorCode());
        assertTrue(result.isDegraded());
        assertEquals("service unavailable", result.getFallbackReason());
    }

    @Test
    void shouldCreateDegradedResultWithErrorCodeAndNullPartialData() {
        AiResult<String> result = AiResultFactory.degraded("timeout", "ERR_TIMEOUT", null);
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertEquals("ERR_TIMEOUT", result.getErrorCode());
        assertTrue(result.isDegraded());
        assertEquals("timeout", result.getFallbackReason());
    }

    @Test
    void shouldSupportDifferentGenericTypes() {
        AiResult<Integer> intResult = AiResultFactory.success(100);
        assertEquals(100, intResult.getData().intValue());

        AiResult<Double> doubleResult = AiResultFactory.success(3.14);
        assertEquals(3.14, doubleResult.getData(), 0.001);
    }
}
