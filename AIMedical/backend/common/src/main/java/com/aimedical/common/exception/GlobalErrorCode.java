package com.aimedical.common.exception;

import lombok.Getter;

@Getter
public enum GlobalErrorCode implements ErrorCode {

    SUCCESS("SUCCESS", "成功"),
    SYSTEM_ERROR("SYSTEM_ERROR", "系统异常"),
    PARAM_INVALID("PARAM_INVALID", "参数校验失败"),
    NOT_FOUND("NOT_FOUND", "资源不存在");

    private final String code;
    private final String message;

    GlobalErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
