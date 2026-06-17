package com.aimedical.modules.patient.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.patient.service.PatientService;
import com.aimedical.modules.patient.service.impl.PatientServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PatientControllerTest {

    private final PatientService service = new PatientServiceImpl();
    private final PatientController controller = new PatientController(service);

    @Test
    void shouldDelegateToServiceAndReturnResult() {
        Result<String> result = controller.placeholder();
        assertEquals("SUCCESS", result.getCode());
        assertEquals("patient placeholder", result.getData());
    }
}
