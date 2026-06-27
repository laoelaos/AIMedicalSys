package com.aimedical.modules.commonmodule.auth.exception;

import org.springframework.security.core.AuthenticationException;

public class AccountDisabledAuthenticationException extends AuthenticationException {

    public AccountDisabledAuthenticationException(String msg) {
        super(msg);
    }

    public AccountDisabledAuthenticationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
