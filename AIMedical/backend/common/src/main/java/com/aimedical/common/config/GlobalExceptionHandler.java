package com.aimedical.common.config;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        return ResponseEntity.badRequest()
                .body(Result.fail(e.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest()
                .body(Result.fail(GlobalErrorCode.PARAM_INVALID));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("Request body malformed", e);
        return ResponseEntity.badRequest()
                .body(Result.fail(GlobalErrorCode.PARAM_INVALID));
    }

    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<Result<Void>> handleMessageNotWritable(HttpMessageNotWritableException e) {
        log.error("Response body serialization failed", e);
        return ResponseEntity.status(500)
                .body(Result.fail(GlobalErrorCode.SYSTEM_ERROR));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("System exception", e);
        return ResponseEntity.status(500)
                .body(Result.fail(GlobalErrorCode.SYSTEM_ERROR));
    }
}
