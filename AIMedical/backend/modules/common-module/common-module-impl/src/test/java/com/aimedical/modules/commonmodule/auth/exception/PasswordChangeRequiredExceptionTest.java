package com.aimedical.modules.commonmodule.auth.exception;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class PasswordChangeRequiredExceptionTest {

    @Test
    void shouldPreserveMessage() {
        PasswordChangeRequiredException ex = new PasswordChangeRequiredException("test msg");
        assertEquals("test msg", ex.getMessage());
    }

    @Test
    void shouldPreserveCause() {
        RuntimeException cause = new RuntimeException("cause");
        PasswordChangeRequiredException ex = new PasswordChangeRequiredException("msg", cause);
        assertSame(cause, ex.getCause());
    }

    @Test
    void shouldBeInstanceOfAccessDeniedException() {
        PasswordChangeRequiredException ex = new PasswordChangeRequiredException("msg");
        assertInstanceOf(AccessDeniedException.class, ex);
    }
}
