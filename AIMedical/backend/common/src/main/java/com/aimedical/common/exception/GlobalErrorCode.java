package com.aimedical.common.exception;

import lombok.Getter;

@Getter
public enum GlobalErrorCode implements ErrorCode {

    SUCCESS("SUCCESS", "成功"),
    SYSTEM_ERROR("SYSTEM_ERROR", "系统异常"),
    PARAM_INVALID("PARAM_INVALID", "参数校验失败"),
    NOT_FOUND("NOT_FOUND", "资源不存在"),
    UNAUTHORIZED("UNAUTHORIZED", "未认证或令牌已失效"),
    FORBIDDEN("FORBIDDEN", "无权限访问"),
    LOGIN_FAILED("LOGIN_FAILED", "用户名或密码错误"),
    ACCOUNT_DISABLED("ACCOUNT_DISABLED", "账户已被管理员停用"),
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", "账户已锁定，请{锁定时间}后重试"),
    RATE_LIMITED("RATE_LIMITED", "登录尝试过于频繁，请稍后重试"),
    RATE_LIMITED_GLOBAL("RATE_LIMITED_GLOBAL", "请求过于频繁，请稍后重试"),
    PASSWORD_TOO_SHORT("PASSWORD_TOO_SHORT", "密码长度不能少于8位"),
    PASSWORD_TOO_LONG("PASSWORD_TOO_LONG", "密码长度不能超过64位"),
    PASSWORD_WEAK("PASSWORD_WEAK", "密码不符合复杂度要求"),
    PASSWORD_CONTAINS_USERNAME("PASSWORD_CONTAINS_USERNAME", "密码不能包含用户名"),
    PASSWORD_COMMON("PASSWORD_COMMON", "密码过于常见"),
    TOKEN_REFRESH_FAILED("TOKEN_REFRESH_FAILED", "令牌刷新失败，请重新登录"),
    PASSWORD_CHANGE_REQUIRED("PASSWORD_CHANGE_REQUIRED", "需要修改密码"),
    CHILDREN_EXIST("CHILDREN_EXIST", "存在子菜单，无法删除"),
    PASSWORD_MISMATCH("PASSWORD_MISMATCH", "旧密码不正确");

    private final String code;
    private final String message;

    GlobalErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
