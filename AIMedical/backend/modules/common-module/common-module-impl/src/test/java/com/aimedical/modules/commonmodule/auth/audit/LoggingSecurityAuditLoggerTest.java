package com.aimedical.modules.commonmodule.auth.audit;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoggingSecurityAuditLoggerTest {

    private final LoggingSecurityAuditLogger logger = new LoggingSecurityAuditLogger();
    private Logger auditLogger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        auditLogger = (Logger) LoggerFactory.getLogger("SECURITY_AUDIT");
        listAppender = new ListAppender<>();
        listAppender.start();
        auditLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        auditLogger.detachAndStopAllAppenders();
    }

    @Test
    void logAudit_shouldNotThrowForFullEvent() {
        SecurityAuditEvent event = SecurityAuditEvent.now(
                SecurityAuditEventType.LOGIN_SUCCESS, 1L, "testuser", "127.0.0.1", true, null, null, "jti-xxx");
        assertDoesNotThrow(() -> logger.logAudit(event));
    }

    @Test
    void logAudit_shouldNotThrowForFailedEvent() {
        SecurityAuditEvent event = SecurityAuditEvent.now(
                SecurityAuditEventType.LOGIN_FAILED, null, null, "192.168.1.1", false, "BAD_CREDENTIALS", null, null);
        assertDoesNotThrow(() -> logger.logAudit(event));
    }

    @Test
    void logAudit_shouldNotThrowWhenAllFieldsPresent() {
        SecurityAuditEvent event = SecurityAuditEvent.now(
                SecurityAuditEventType.LOGOUT, 2L, "johndoe", "10.0.0.1", true, null, "abc123***", null);
        assertDoesNotThrow(() -> logger.logAudit(event));
    }

    @Test
    void logAudit_shouldNotThrowOnNullEvent() {
        assertDoesNotThrow(() -> logger.logAudit(null));
    }

    @Test
    void logAudit_shouldWriteExpectedFormat() {
        SecurityAuditEvent event = SecurityAuditEvent.now(
                SecurityAuditEventType.LOGIN_SUCCESS, 1L, "testuser", "127.0.0.1", true, null, null, null);
        logger.logAudit(event);

        List<ILoggingEvent> logs = listAppender.list;
        assertEquals(1, logs.size());
        String msg = logs.get(0).getFormattedMessage();
        assertTrue(msg.startsWith("timestamp="));
        assertTrue(msg.contains(" eventType=LOGIN_SUCCESS"));
        assertTrue(msg.contains(" userId=1"));
        assertTrue(msg.contains(" username=testuser"));
        assertTrue(msg.contains(" clientIp=127.0.0.1"));
        assertTrue(msg.contains(" success=true"));
        assertFalse(msg.contains("failureReason="));
        assertFalse(msg.contains("refreshTokenMasked="));
        assertFalse(msg.contains("newJti="));
    }

    @Test
    void logAudit_shouldIncludeOptionalFieldsWhenPresent() {
        SecurityAuditEvent event = SecurityAuditEvent.now(
                SecurityAuditEventType.LOGIN_FAILED, null, null, "10.0.0.1", false, "BAD_CREDENTIALS", "abc123***", "new-jti");
        logger.logAudit(event);

        List<ILoggingEvent> logs = listAppender.list;
        assertEquals(1, logs.size());
        String msg = logs.get(0).getFormattedMessage();
        assertTrue(msg.contains("failureReason=BAD_CREDENTIALS"));
        assertTrue(msg.contains("refreshTokenMasked=abc123***"));
        assertTrue(msg.contains("newJti=new-jti"));
    }

    @Test
    void logAudit_shouldHandleNullUsername() {
        SecurityAuditEvent event = SecurityAuditEvent.now(
                SecurityAuditEventType.LOGIN_FAILED, null, null, "10.0.0.1", false, "USER_NOT_FOUND", null, null);
        assertDoesNotThrow(() -> logger.logAudit(event));

        List<ILoggingEvent> logs = listAppender.list;
        assertEquals(1, logs.size());
        String msg = logs.get(0).getFormattedMessage();
        assertTrue(msg.contains(" username=null"));
    }

    @Test
    void logAudit_shouldLogToSECURITY_AUDITLogger() {
        SecurityAuditEvent event = SecurityAuditEvent.now(
                SecurityAuditEventType.LOGOUT, 2L, "user", "10.0.0.1", true, null, null, null);
        logger.logAudit(event);

        List<ILoggingEvent> logs = listAppender.list;
        assertEquals(1, logs.size());
        assertEquals("SECURITY_AUDIT", logs.get(0).getLoggerName());
    }

}
