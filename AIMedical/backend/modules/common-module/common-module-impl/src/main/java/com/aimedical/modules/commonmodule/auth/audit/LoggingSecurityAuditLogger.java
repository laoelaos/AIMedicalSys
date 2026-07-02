package com.aimedical.modules.commonmodule.auth.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class LoggingSecurityAuditLogger implements SecurityAuditLogger {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("SECURITY_AUDIT");
    private static final Logger log = LoggerFactory.getLogger(LoggingSecurityAuditLogger.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            .withZone(ZoneId.systemDefault());

    @Override
    public void logAudit(SecurityAuditEvent event) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("timestamp=").append(ISO_FORMATTER.format(Instant.ofEpochMilli(event.timestamp())));
            sb.append(" eventType=").append(event.eventType());
            sb.append(" userId=").append(event.userId());
            sb.append(" username=").append(maskUsername(event.username()));
            sb.append(" clientIp=").append(event.clientIp());
            sb.append(" success=").append(event.success());
            if (event.failureReason() != null) {
                sb.append(" failureReason=").append(event.failureReason());
            }
            if (event.refreshTokenMasked() != null) {
                sb.append(" refreshTokenMasked=").append(event.refreshTokenMasked());
            }
            if (event.newJti() != null) {
                sb.append(" newJti=").append(event.newJti());
            }
            AUDIT_LOG.info(sb.toString());
        } catch (Exception e) {
            log.warn("Audit log write failed", e);
        }
    }

    /**
     * 对用户名做脱敏：11 位手机号掩码为前 3 + **** + 后 4，其余保持原值。
     * 避免患者手机号明文写入审计日志。
     */
    private String maskUsername(String username) {
        if (username == null) {
            return null;
        }
        if (username.matches("\\d{11}")) {
            return username.substring(0, 3) + "****" + username.substring(7);
        }
        return username;
    }
}
