package com.aimedical.modules.commonmodule.auth.exception;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.AuthenticationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class AccountDisabledAuthenticationExceptionTest {

    @Test
    void shouldPreserveMessage() {
        AccountDisabledAuthenticationException ex = new AccountDisabledAuthenticationException("test msg");
        assertEquals("test msg", ex.getMessage());
    }

    @Test
    void shouldPreserveCause() {
        RuntimeException cause = new RuntimeException("cause");
        AccountDisabledAuthenticationException ex = new AccountDisabledAuthenticationException("msg", cause);
        assertSame(cause, ex.getCause());
    }

    @Test
    void shouldBeInstanceOfAuthenticationException() {
        AccountDisabledAuthenticationException ex = new AccountDisabledAuthenticationException("msg");
        assertInstanceOf(AuthenticationException.class, ex);
    }
}
