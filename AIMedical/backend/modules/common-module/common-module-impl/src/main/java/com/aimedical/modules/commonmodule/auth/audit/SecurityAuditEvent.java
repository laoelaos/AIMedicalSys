package com.aimedical.modules.commonmodule.auth.audit;

public record SecurityAuditEvent(
    SecurityAuditEventType eventType,
    Long userId,
    String username,
    String clientIp,
    boolean success,
    String failureReason,
    String refreshTokenMasked,
    String newJti,
    long timestamp
) {
    public static SecurityAuditEvent now(
        SecurityAuditEventType eventType,
        Long userId,
        String username,
        String clientIp,
        boolean success,
        String failureReason,
        String refreshTokenMasked,
        String newJti
    ) {
        return new SecurityAuditEvent(eventType, userId, username, clientIp,
            success, failureReason, refreshTokenMasked, newJti, System.currentTimeMillis());
    }
}
