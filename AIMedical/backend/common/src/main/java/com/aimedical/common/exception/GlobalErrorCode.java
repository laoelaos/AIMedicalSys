package com.aimedical.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum GlobalErrorCode implements ErrorCode {

    SUCCESS("SUCCESS", "成功", HttpStatus.OK),
    SYSTEM_ERROR("SYSTEM_ERROR", "系统异常", HttpStatus.INTERNAL_SERVER_ERROR),
    PARAM_INVALID("PARAM_INVALID", "参数校验失败", HttpStatus.BAD_REQUEST),
    NOT_FOUND("NOT_FOUND", "资源不存在", HttpStatus.NOT_FOUND),
    UNAUTHORIZED("UNAUTHORIZED", "未认证或令牌已失效", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", "无权限访问", HttpStatus.FORBIDDEN),
    LOGIN_FAILED("LOGIN_FAILED", "用户名或密码错误", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED("ACCOUNT_DISABLED", "账户已被管理员停用", HttpStatus.FORBIDDEN),
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", "账户已锁定，请{锁定时间}后重试", HttpStatus.LOCKED),
    RATE_LIMITED("RATE_LIMITED", "登录尝试过于频繁，请稍后重试", HttpStatus.TOO_MANY_REQUESTS),
    RATE_LIMITED_GLOBAL("RATE_LIMITED_GLOBAL", "请求过于频繁，请稍后重试", HttpStatus.TOO_MANY_REQUESTS),
    PASSWORD_TOO_SHORT("PASSWORD_TOO_SHORT", "密码长度不能少于8位"),
    PASSWORD_TOO_LONG("PASSWORD_TOO_LONG", "密码长度不能超过64位"),
    PASSWORD_WEAK("PASSWORD_WEAK", "密码不符合复杂度要求"),
    PASSWORD_CONTAINS_USERNAME("PASSWORD_CONTAINS_USERNAME", "密码不能包含用户名"),
    PASSWORD_COMMON("PASSWORD_COMMON", "密码过于常见"),
    TOKEN_REFRESH_FAILED("TOKEN_REFRESH_FAILED", "令牌刷新失败，请重新登录", HttpStatus.UNAUTHORIZED),
    PASSWORD_CHANGE_REQUIRED("PASSWORD_CHANGE_REQUIRED", "需要修改密码", HttpStatus.FORBIDDEN),
    CHILDREN_EXIST("CHILDREN_EXIST", "存在子菜单，无法删除"),
    PASSWORD_MISMATCH("PASSWORD_MISMATCH", "旧密码不正确"),
    DUPLICATE("DUPLICATE", "重复提交"),
    REGISTRATION_STATUS_INVALID("REGISTRATION_STATUS_INVALID", "当前挂号状态不允许此操作"),
    REGISTRATION_CANCEL_FORBIDDEN("REGISTRATION_CANCEL_FORBIDDEN", "预约时间距现在不足2小时，无法在线取消，请到窗口线下办理"),
    ORDER_STATUS_INVALID("ORDER_STATUS_INVALID", "当前订单状态不允许此操作"),
    ORDER_ITEM_EMPTY("ORDER_ITEM_EMPTY", "订单至少需要包含一个项目"),
    CHARGE_PRE_ORDER_EXISTS("CHARGE_PRE_ORDER_EXISTS", "该订单已生成收费前置单，不可重复生成"),
    TRIAGE_RECORD_EXISTS("TRIAGE_RECORD_EXISTS", "该挂号已存在分诊记录，不可重复创建");

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    GlobalErrorCode(String code, String message) {
        this(code, message, HttpStatus.BAD_REQUEST);
    }

    GlobalErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
