package com.aimedical.common.config;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.ErrorCode;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.common.result.Result;
import com.aimedical.common.util.SimpleMessageInterpolator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(new SimpleMessageInterpolator());

    private static final ErrorCode TEST_ERROR = new ErrorCode() {
        @Override
        public String getCode() {
            return "BIZ_ERR";
        }

        @Override
        public String getMessage() {
            return "业务异常";
        }
    };

    private static final ErrorCode NUMBERED_TEMPLATE = new ErrorCode() {
        @Override
        public String getCode() {
            return "NUM_ERR";
        }

        @Override
        public String getMessage() {
            return "订单{0}已过期，剩余{1}天";
        }
    };

    @Test
    void shouldHandleBusinessExceptionWith400() {
        BusinessException ex = new BusinessException(TEST_ERROR);
        ResponseEntity<Result<Void>> response = handler.handleBusinessException(ex);
        assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
        Result<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals("BIZ_ERR", body.getCode());
        assertEquals("业务异常", body.getMessage());
        assertNull(body.getData());
    }

    @Test
    void shouldInterpolateAccountLockedMessage() {
        BusinessException ex = new BusinessException(GlobalErrorCode.ACCOUNT_LOCKED, "30分钟");
        ResponseEntity<Result<Void>> response = handler.handleBusinessException(ex);
        assertEquals(HttpStatusCode.valueOf(423), response.getStatusCode());
        Result<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals("ACCOUNT_LOCKED", body.getCode());
        assertEquals("账户已锁定，请30分钟后重试", body.getMessage());
        assertNull(body.getData());
    }

    @Test
    void shouldReturn429ForRateLimited() {
        BusinessException ex = new BusinessException(GlobalErrorCode.RATE_LIMITED);
        ResponseEntity<Result<Void>> response = handler.handleBusinessException(ex);
        assertEquals(HttpStatusCode.valueOf(429), response.getStatusCode());
        Result<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals("RATE_LIMITED", body.getCode());
        assertEquals("登录尝试过于频繁，请稍后重试", body.getMessage());
        assertNull(body.getData());
    }

    @Test
    void shouldReturn401ForTokenRefreshFailed() {
        BusinessException ex = new BusinessException(GlobalErrorCode.TOKEN_REFRESH_FAILED);
        ResponseEntity<Result<Void>> response = handler.handleBusinessException(ex);
        assertEquals(HttpStatusCode.valueOf(401), response.getStatusCode());
        Result<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals("TOKEN_REFRESH_FAILED", body.getCode());
        assertEquals("令牌刷新失败，请重新登录", body.getMessage());
        assertNull(body.getData());
    }

    @Test
    void shouldHandleBusinessExceptionWithEmptyArgs() {
        BusinessException ex = new BusinessException(TEST_ERROR, new Object[0]);
        ResponseEntity<Result<Void>> response = handler.handleBusinessException(ex);
        assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
        Result<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals("业务异常", body.getMessage());
        assertNull(body.getData());
    }

    @Test
    void shouldHandleBusinessExceptionWithNumberedPlaceholder() {
        BusinessException ex = new BusinessException(NUMBERED_TEMPLATE, "ORD-001", "3");
        ResponseEntity<Result<Void>> response = handler.handleBusinessException(ex);
        Result<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals("NUM_ERR", body.getCode());
        assertEquals("订单ORD-001已过期，剩余3天", body.getMessage());
    }

    @Test
    void shouldInterpolateAccountLockedMessage_logsInterpolatedMessage() {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            BusinessException ex = new BusinessException(GlobalErrorCode.ACCOUNT_LOCKED, "30分钟");
            handler.handleBusinessException(ex);
            assertEquals(1, appender.list.size());
            assertEquals(Level.WARN, appender.list.get(0).getLevel());
            String logMsg = appender.list.get(0).getFormattedMessage();
            assertTrue(logMsg.contains("ACCOUNT_LOCKED"));
            assertTrue(logMsg.contains("30分钟"));
        } finally {
            appender.stop();
            logger.detachAppender(appender);
        }
    }

    @Test
    void shouldLogOriginalTemplateWhenNoArgs() {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            BusinessException ex = new BusinessException(TEST_ERROR);
            handler.handleBusinessException(ex);
            assertEquals(1, appender.list.size());
            assertEquals(Level.WARN, appender.list.get(0).getLevel());
            String logMsg = appender.list.get(0).getFormattedMessage();
            assertTrue(logMsg.contains("BIZ_ERR"));
            assertTrue(logMsg.contains("业务异常"));
        } finally {
            appender.stop();
            logger.detachAppender(appender);
        }
    }

    @Test
    void shouldLogInterpolatedMessageWithNumberedPlaceholders() {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            BusinessException ex = new BusinessException(NUMBERED_TEMPLATE, "ORD-001", "3");
            handler.handleBusinessException(ex);
            assertEquals(1, appender.list.size());
            assertEquals(Level.WARN, appender.list.get(0).getLevel());
            String logMsg = appender.list.get(0).getFormattedMessage();
            assertTrue(logMsg.contains("NUM_ERR"));
            assertTrue(logMsg.contains("ORD-001"));
            assertTrue(logMsg.contains("剩余3天"));
        } finally {
            appender.stop();
            logger.detachAppender(appender);
        }
    }

    @Test
    void shouldLogOriginalTemplateForRateLimited() {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            BusinessException ex = new BusinessException(GlobalErrorCode.RATE_LIMITED);
            handler.handleBusinessException(ex);
            assertEquals(1, appender.list.size());
            assertEquals(Level.WARN, appender.list.get(0).getLevel());
            String logMsg = appender.list.get(0).getFormattedMessage();
            assertTrue(logMsg.contains("RATE_LIMITED"));
            assertTrue(logMsg.contains("登录尝试过于频繁，请稍后重试"));
        } finally {
            appender.stop();
            logger.detachAppender(appender);
        }
    }

    @Test
    void shouldHandleValidationExceptionWith400() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "testObject");
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        ResponseEntity<Result<Void>> response = handler.handleValidationException(ex);
        assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
        Result<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals(GlobalErrorCode.PARAM_INVALID.getCode(), body.getCode());
        assertEquals(GlobalErrorCode.PARAM_INVALID.getMessage(), body.getMessage());
    }

    @Test
    void shouldHandleMessageNotReadableWith400() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Malformed JSON");
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            ResponseEntity<Result<Void>> response = handler.handleMessageNotReadable(ex);
            assertEquals(1, appender.list.size());
            assertEquals(Level.WARN, appender.list.get(0).getLevel());
            assertEquals("Request body malformed", appender.list.get(0).getFormattedMessage());
            assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
            Result<Void> body = response.getBody();
            assertNotNull(body);
            assertEquals(GlobalErrorCode.PARAM_INVALID.getCode(), body.getCode());
            assertEquals(GlobalErrorCode.PARAM_INVALID.getMessage(), body.getMessage());
            assertNull(body.getData());
        } finally {
            appender.stop();
            logger.detachAppender(appender);
        }
    }

    @Test
    void shouldHandleMessageNotWritableWith500() {
        HttpMessageNotWritableException ex = new HttpMessageNotWritableException("Serialization failed");
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            ResponseEntity<Result<Void>> response = handler.handleMessageNotWritable(ex);
            assertEquals(1, appender.list.size());
            assertEquals(Level.ERROR, appender.list.get(0).getLevel());
            assertEquals("Response body serialization failed", appender.list.get(0).getFormattedMessage());
            assertEquals(HttpStatusCode.valueOf(500), response.getStatusCode());
            Result<Void> body = response.getBody();
            assertNotNull(body);
            assertEquals(GlobalErrorCode.SYSTEM_ERROR.getCode(), body.getCode());
            assertEquals(GlobalErrorCode.SYSTEM_ERROR.getMessage(), body.getMessage());
            assertNull(body.getData());
        } finally {
            appender.stop();
            logger.detachAppender(appender);
        }
    }

    @Test
    void shouldHandleTypeMismatchWith400() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Long.class, "id", null, null);
        ResponseEntity<Result<Void>> response = handler.handleTypeMismatch(ex);
        assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
        Result<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals(GlobalErrorCode.PARAM_INVALID.getCode(), body.getCode());
        assertNull(body.getData());
    }

    @Test
    void shouldHandleMethodNotSupportedWith405() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST");
        ResponseEntity<Result<Void>> response = handler.handleMethodNotSupported(ex);
        assertEquals(HttpStatusCode.valueOf(405), response.getStatusCode());
        Result<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals(GlobalErrorCode.PARAM_INVALID.getCode(), body.getCode());
        assertTrue(body.getMessage().contains("POST"));
        assertNull(body.getData());
    }

    @Test
    void shouldHandleAccessDeniedWith403() {
        AccessDeniedException ex = new AccessDeniedException("Access is denied");
        ResponseEntity<Result<Void>> response = handler.handleAccessDenied(ex);
        assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
        Result<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals(GlobalErrorCode.FORBIDDEN.getCode(), body.getCode());
        assertEquals(GlobalErrorCode.FORBIDDEN.getMessage(), body.getMessage());
        assertNull(body.getData());
    }

    @Test
    void shouldHandleMissingParamWith400() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("name", "String");
        ResponseEntity<Result<Void>> response = handler.handleMissingParam(ex);
        assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
        Result<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals(GlobalErrorCode.PARAM_INVALID.getCode(), body.getCode());
        assertTrue(body.getMessage().contains("name"));
        assertNull(body.getData());
    }

    @Test
    void shouldHandleOptimisticLockWith409() {
        ObjectOptimisticLockingFailureException ex =
                new ObjectOptimisticLockingFailureException("Optimistic lock failed", new RuntimeException("version mismatch"));
        ResponseEntity<Result<Void>> response = handler.handleOptimisticLock(ex);
        assertEquals(HttpStatusCode.valueOf(409), response.getStatusCode());
        Result<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals(GlobalErrorCode.CONFLICT.getCode(), body.getCode());
        assertNull(body.getData());
    }

    @Test
    void shouldHandleDataIntegrityViolationWith409() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("Duplicate key", new RuntimeException("Unique constraint violation"));
        ResponseEntity<Result<Void>> response = handler.handleDataIntegrityViolation(ex);
        assertEquals(HttpStatusCode.valueOf(409), response.getStatusCode());
        Result<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals(GlobalErrorCode.CONFLICT.getCode(), body.getCode());
        assertNull(body.getData());
    }

    @Test
    void shouldHandleGenericExceptionWith500() {
        Exception ex = new RuntimeException("unexpected");
        ResponseEntity<Result<Void>> response = handler.handleException(ex);
        assertEquals(HttpStatusCode.valueOf(500), response.getStatusCode());
        Result<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals(GlobalErrorCode.SYSTEM_ERROR.getCode(), body.getCode());
        assertEquals(GlobalErrorCode.SYSTEM_ERROR.getMessage(), body.getMessage());
        assertNull(body.getData());
    }
}
