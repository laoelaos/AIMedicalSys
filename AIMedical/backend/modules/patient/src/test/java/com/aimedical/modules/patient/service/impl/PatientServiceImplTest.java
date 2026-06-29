package com.aimedical.modules.patient.service.impl;

import com.aimedical.common.result.Result;
import com.aimedical.modules.patient.repository.PatientRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PatientServiceImplTest {

    private final PatientRepository repository = mock(PatientRepository.class);
    private final PatientServiceImpl service = new PatientServiceImpl(repository);

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
