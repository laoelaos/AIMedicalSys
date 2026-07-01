package com.aimedical.modules.prescription;

import com.aimedical.common.exception.ErrorCode;

public enum PrescriptionErrorCode implements ErrorCode {

    RX_AUDIT_BLOCKED("RX_AUDIT_BLOCKED", "审核阻断"),
    RX_AUDIT_PRESCRIPTION_MODIFIED("RX_AUDIT_PRESCRIPTION_MODIFIED", "处方已变更"),
    RX_AUDIT_CONCURRENT_SUBMIT("RX_AUDIT_CONCURRENT_SUBMIT", "并发提交冲突"),
    RX_AUDIT_FORCE_SUBMIT_INVALID("RX_AUDIT_FORCE_SUBMIT_INVALID", "强制提交无效，仅 WARN 级别可强制提交"),
    RX_AUDIT_REVOKE_NOT_WARN("RX_AUDIT_REVOKE_NOT_WARN", "非 WARN 级别无法撤销"),
    RX_AUDIT_REVOKE_ALREADY_REVOKED("RX_AUDIT_REVOKE_ALREADY_REVOKED", "审核已撤销"),
    RX_AUDIT_REVOKE_NOT_FOUND("RX_AUDIT_REVOKE_NOT_FOUND", "审核记录不存在"),

    RX_ASSIST_AI_NO_RECOMMENDATION("RX_ASSIST_AI_NO_RECOMMENDATION", "AI 暂无可推荐药品"),
    RX_ASSIST_SUGGESTION_NOT_FOUND("RX_ASSIST_SUGGESTION_NOT_FOUND", "异步 AI 建议不存在或已过期"),
    RX_ASSIST_DOSE_STANDARD_NOT_FOUND("RX_ASSIST_DOSE_STANDARD_NOT_FOUND", "剂量标准未找到");

    private final String code;
    private final String message;

    PrescriptionErrorCode(String code, String message) {
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
