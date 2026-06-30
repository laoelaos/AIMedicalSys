package com.aimedical.modules.doctor.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.doctor.service.DoctorService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DoctorControllerTest {

    private final DoctorService service = mock(DoctorService.class);
    private final DoctorController controller = new DoctorController(service);

    @Test
    void shouldDelegateToServiceAndReturnResult() {
        when(service.getPlaceholder()).thenReturn(Result.success("doctor placeholder"));

        Result<String> result = controller.placeholder();
        assertEquals("SUCCESS", result.getCode());
        assertEquals("doctor placeholder", result.getData());
    }
}
