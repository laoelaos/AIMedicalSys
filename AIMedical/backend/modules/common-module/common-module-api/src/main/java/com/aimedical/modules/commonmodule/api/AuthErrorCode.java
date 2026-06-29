package com.aimedical.modules.commonmodule.api;

import com.aimedical.common.exception.ErrorCode;

/**
 * 认证授权通用错误码。
 */
public enum AuthErrorCode implements ErrorCode {

    AUTH_MOBILE_EXISTS("AUTH_MOBILE_EXISTS", "该手机号已注册"),
    AUTH_LOGIN_FAILED("AUTH_LOGIN_FAILED", "用户名或密码错误"),
    AUTH_ACCOUNT_DISABLED("AUTH_ACCOUNT_DISABLED", "账户已被禁用"),
    AUTH_TOKEN_INVALID("AUTH_TOKEN_INVALID", "令牌无效或已过期");

    private final String code;
    private final String message;

    AuthErrorCode(String code, String message) {
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
