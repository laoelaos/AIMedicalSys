package com.aimedical.modules.commonmodule.auth.exception;

import org.springframework.security.access.AccessDeniedException;

public class PasswordChangeRequiredException extends AccessDeniedException {

    public PasswordChangeRequiredException(String msg) {
        super(msg);
    }

    public PasswordChangeRequiredException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
