package com.aimedical.modules.patient.service.impl;

import com.aimedical.common.result.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PatientServiceImplTest {

    private final PatientServiceImpl service = new PatientServiceImpl();

    @Test
    void shouldReturnSuccessCode() {
        Result<String> result = service.getPlaceholder();
        assertEquals("SUCCESS", result.getCode());
    }

    @Test
    void shouldReturnPatientPlaceholderData() {
        Result<String> result = service.getPlaceholder();
        assertEquals("patient placeholder", result.getData());
    }

    @Test
    void shouldReturnSuccessMessage() {
        Result<String> result = service.getPlaceholder();
        assertEquals("成功", result.getMessage());
    }
}
