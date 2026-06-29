package com.aimedical.modules.doctor.service.impl;

import com.aimedical.common.result.Result;
import com.aimedical.modules.doctor.repository.DoctorRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DoctorServiceImplTest {

    private final DoctorRepository repository = mock(DoctorRepository.class);
    private final DoctorServiceImpl service = new DoctorServiceImpl(repository);

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
    void shouldReturnSuccessMessage() {
        Result<String> result = service.getPlaceholder();
        assertEquals("成功", result.getMessage());
    }
}
