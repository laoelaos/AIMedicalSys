package com.aimedical.modules.ai.api;

import lombok.Data;

@Data
public class AiResult<T> {

    private boolean success;
    private T data;
    private String errorCode;
    private boolean degraded;
    private String fallbackReason;

    public AiResult() {
    }

    public AiResult(boolean success, T data, String errorCode, boolean degraded, String fallbackReason) {
        this.success = success;
        this.data = data;
        this.errorCode = errorCode;
        this.degraded = degraded;
        this.fallbackReason = fallbackReason;
    }

    public static <T> AiResult<T> success(T data) {
        return new AiResult<>(true, data, null, false, null);
    }

    public static <T> AiResult<T> failure(String errorCode) {
        return new AiResult<>(false, null, errorCode, false, null);
    }

    public static <T> AiResult<T> degraded(String fallbackReason) {
        return new AiResult<>(false, null, null, true, fallbackReason);
    }
}
