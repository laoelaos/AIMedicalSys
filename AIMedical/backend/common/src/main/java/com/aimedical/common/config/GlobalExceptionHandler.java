package com.aimedical.common.config;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.ErrorCode;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.common.result.Result;
import com.aimedical.common.util.MessageInterpolator;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageInterpolator messageInterpolator;

    public GlobalExceptionHandler(MessageInterpolator messageInterpolator) {
        this.messageInterpolator = messageInterpolator;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        HttpStatus status = resolveHttpStatus(errorCode);
        String message = messageInterpolator.interpolate(errorCode.getMessage(), e.getArgs());
        log.warn("Business exception: code={}, message={}", errorCode.getCode(), message);
        return ResponseEntity.status(status)
                .body(Result.fail(errorCode.getCode(), message));
    }

    /**
     * 根据ErrorCode映射对应的HTTP状态码
     *
     * <p>区分认证/授权/资源不存在/参数错误等场景，避免所有业务异常统一返回400。
     *
     * @param errorCode 错误码
     * @return 对应的HTTP状态码
     */
    private HttpStatus resolveHttpStatus(ErrorCode errorCode) {
        String code = errorCode.getCode();
        if (GlobalErrorCode.UNAUTHORIZED.getCode().equals(code)) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (GlobalErrorCode.FORBIDDEN.getCode().equals(code)) {
            return HttpStatus.FORBIDDEN;
        }
        if (GlobalErrorCode.NOT_FOUND.getCode().equals(code)) {
            return HttpStatus.NOT_FOUND;
        }
        if (GlobalErrorCode.PARAM_INVALID.getCode().equals(code)) {
            return HttpStatus.BAD_REQUEST;
        }
        if (GlobalErrorCode.SYSTEM_ERROR.getCode().equals(code)) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (GlobalErrorCode.ACCOUNT_LOCKED.getCode().equals(code)) {
            return HttpStatus.LOCKED;
        }
        if (GlobalErrorCode.RATE_LIMITED.getCode().equals(code)
                || GlobalErrorCode.RATE_LIMITED_GLOBAL.getCode().equals(code)) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        if (GlobalErrorCode.TOKEN_REFRESH_FAILED.getCode().equals(code)) {
            return HttpStatus.UNAUTHORIZED;
        }
        // 其他业务错误码默认返回400
        return HttpStatus.BAD_REQUEST;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (detail.isEmpty()) {
            detail = GlobalErrorCode.PARAM_INVALID.getMessage();
        }
        log.warn("Validation failed: {}", detail);
        return ResponseEntity.badRequest()
                .body(Result.fail(GlobalErrorCode.PARAM_INVALID.getCode(), detail));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String detail = e.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("Constraint violation: {}", detail);
        return ResponseEntity.badRequest()
                .body(Result.fail(GlobalErrorCode.PARAM_INVALID.getCode(), detail));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("Request body malformed", e);
        return ResponseEntity.badRequest()
                .body(Result.fail(GlobalErrorCode.PARAM_INVALID));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("Argument type mismatch: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(Result.fail(GlobalErrorCode.PARAM_INVALID));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("Method not supported: {}", e.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.fail(GlobalErrorCode.PARAM_INVALID.getCode(),
                        "请求方法不支持: " + e.getMethod()));
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
