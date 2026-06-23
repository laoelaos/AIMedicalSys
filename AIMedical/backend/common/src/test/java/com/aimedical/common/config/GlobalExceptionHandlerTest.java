package com.aimedical.common.config;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.ErrorCode;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.common.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

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
