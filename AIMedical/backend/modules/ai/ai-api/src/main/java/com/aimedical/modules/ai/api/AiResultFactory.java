package com.aimedical.modules.ai.api;

public final class AiResultFactory {

    private AiResultFactory() {
    }

    public static <T> AiResult<T> failure(String errorCode, T partialData) {
        return new AiResult<>(false, partialData, errorCode, false, null);
    }

    public static <T> AiResult<T> degraded(String fallbackReason, T partialData) {
        return new AiResult<>(false, partialData, null, true, fallbackReason);
    }

    public static <T> AiResult<T> degraded(String fallbackReason, String errorCode, T partialData) {
        return new AiResult<>(false, partialData, errorCode, true, fallbackReason);
    }

    public static <T> AiResult<T> failure(String errorCode) {
        return new AiResult<>(false, null, errorCode, false, null);
    }

    public static <T> AiResult<T> success(T data) {
        return new AiResult<>(true, data, null, false, null);
    }
}
