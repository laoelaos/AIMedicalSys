package com.aimedical.common.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    String getCode();
    String getMessage();

    default HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
