package com.aimedical.modules.commonmodule.auth.audit;

public enum SecurityAuditEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    LOGOUT,
    TOKEN_REFRESH_SUCCESS,
    TOKEN_REFRESH_REJECTED,
    PASSWORD_CHANGED,
    ACCOUNT_LOCKED
}
