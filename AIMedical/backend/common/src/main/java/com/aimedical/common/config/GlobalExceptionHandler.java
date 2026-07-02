package com.aimedical.common.config;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.ErrorCode;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.common.result.Result;
import com.aimedical.common.util.MessageInterpolator;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
        return errorCode.getHttpStatus();
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

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("No resource found: {}", e.getResourcePath());
        return ResponseEntity.status(404)
                .body(Result.fail(GlobalErrorCode.NOT_FOUND));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.fail(GlobalErrorCode.FORBIDDEN));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("Missing parameter: {}", e.getParameterName());
        return ResponseEntity.badRequest()
                .body(Result.fail(GlobalErrorCode.PARAM_INVALID.getCode(),
                        "缺少必需参数: " + e.getParameterName()));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Result<Void>> handleOptimisticLock(ObjectOptimisticLockingFailureException e) {
        log.warn("Optimistic lock failure: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Result.fail(GlobalErrorCode.CONFLICT));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Data integrity violation: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Result.fail(GlobalErrorCode.CONFLICT));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("System exception", e);
        return ResponseEntity.status(500)
                .body(Result.fail(GlobalErrorCode.SYSTEM_ERROR));
    }
}
