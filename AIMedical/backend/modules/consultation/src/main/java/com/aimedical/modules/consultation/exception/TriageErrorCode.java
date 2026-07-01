package com.aimedical.modules.consultation.exception;

import com.aimedical.common.exception.ErrorCode;

public enum TriageErrorCode implements ErrorCode {
    TRIAGE_SESSION_NOT_FOUND("TRIAGE_SESSION_NOT_FOUND", "分诊会话不存在"),
    TRIAGE_FIELD_COMBINATION_INVALID("TRIAGE_FIELD_COMBINATION_INVALID", "主诉与追问互斥或均缺失");

    private final String code;
    private final String message;

    TriageErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() { return code; }

    @Override
    public String getMessage() { return message; }
}
