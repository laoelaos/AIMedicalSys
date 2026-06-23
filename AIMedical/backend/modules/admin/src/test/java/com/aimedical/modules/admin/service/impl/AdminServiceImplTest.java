package com.aimedical.modules.admin.service.impl;

import com.aimedical.common.result.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdminServiceImplTest {

    private final AdminServiceImpl service = new AdminServiceImpl();

    @Test
    void shouldReturnSuccessCode() {
        Result<String> result = service.getPlaceholder();
        assertEquals("SUCCESS", result.getCode());
    }

    @Test
    void shouldReturnAdminPlaceholderData() {
        Result<String> result = service.getPlaceholder();
        assertEquals("admin placeholder", result.getData());
    }

    @Test
    void shouldReturnNullMessage() {
        Result<String> result = service.getPlaceholder();
        assertNull(result.getMessage());
    }
}
