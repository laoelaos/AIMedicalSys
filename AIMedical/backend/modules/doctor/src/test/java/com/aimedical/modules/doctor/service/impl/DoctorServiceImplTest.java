package com.aimedical.modules.doctor.service.impl;

import com.aimedical.common.result.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DoctorServiceImplTest {

    private final DoctorServiceImpl service = new DoctorServiceImpl();

    @Test
    void shouldReturnSuccessCode() {
        Result<String> result = service.getPlaceholder();
        assertEquals("SUCCESS", result.getCode());
    }

    @Test
    void shouldReturnDoctorPlaceholderData() {
        Result<String> result = service.getPlaceholder();
        assertEquals("doctor placeholder", result.getData());
    }

    @Test
    void shouldReturnNullMessage() {
        Result<String> result = service.getPlaceholder();
        assertNull(result.getMessage());
    }
}
