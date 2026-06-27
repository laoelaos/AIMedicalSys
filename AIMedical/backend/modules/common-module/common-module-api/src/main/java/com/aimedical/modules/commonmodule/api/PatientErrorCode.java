package com.aimedical.modules.commonmodule.api;

import com.aimedical.common.exception.ErrorCode;

public enum PatientErrorCode implements ErrorCode {

    PATIENT_MOBILE_EXISTS("PATIENT_MOBILE_EXISTS", "该手机号已注册"),
    PATIENT_LOGIN_FAILED("PATIENT_LOGIN_FAILED", "手机号或密码错误"),
    PATIENT_ACCOUNT_DISABLED("PATIENT_ACCOUNT_DISABLED", "账户已被禁用"),
    PATIENT_TOKEN_INVALID("PATIENT_TOKEN_INVALID", "令牌无效或已过期"),
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
