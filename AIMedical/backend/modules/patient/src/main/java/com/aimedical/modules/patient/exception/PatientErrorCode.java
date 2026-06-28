package com.aimedical.modules.patient.exception;

import com.aimedical.common.exception.ErrorCode;

/**
 * Patient-specific error codes.
 */
public enum PatientErrorCode implements ErrorCode {

    PATIENT_NOT_FOUND("PATIENT_NOT_FOUND", "患者不存在"),
    PATIENT_HEALTH_RECORD_NOT_FOUND("PATIENT_HEALTH_RECORD_NOT_FOUND", "健康档案记录不存在");

    private final String code;
    private final String message;

    PatientErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
