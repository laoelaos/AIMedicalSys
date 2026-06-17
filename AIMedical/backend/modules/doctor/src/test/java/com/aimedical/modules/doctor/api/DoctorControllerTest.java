package com.aimedical.modules.doctor.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.doctor.service.DoctorService;
import com.aimedical.modules.doctor.service.impl.DoctorServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DoctorControllerTest {

    private final DoctorService service = new DoctorServiceImpl();
    private final DoctorController controller = new DoctorController(service);

    @Test
    void shouldDelegateToServiceAndReturnResult() {
        Result<String> result = controller.placeholder();
        assertEquals("SUCCESS", result.getCode());
        assertEquals("doctor placeholder", result.getData());
    }
}
