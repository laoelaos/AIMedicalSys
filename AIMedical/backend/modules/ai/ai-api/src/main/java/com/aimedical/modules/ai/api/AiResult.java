package com.aimedical.modules.ai.api;

import java.util.Objects;

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
        return new AiResult<>(true, Objects.requireNonNull(data), null, false, null);
    }

    public static <T> AiResult<T> failure(String errorCode) {
        return new AiResult<>(false, null, errorCode, false, null);
    }

    public static <T> AiResult<T> degraded(String fallbackReason) {
        return new AiResult<>(false, null, null, true, fallbackReason);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public boolean isDegraded() {
        return degraded;
    }

    public void setDegraded(boolean degraded) {
        this.degraded = degraded;
    }

    public String getFallbackReason() {
        return fallbackReason;
    }

    public void setFallbackReason(String fallbackReason) {
        this.fallbackReason = fallbackReason;
    }
}