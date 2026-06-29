package com.aimedical.modules.patient.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.patient.service.PatientService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PatientControllerTest {

    private final PatientService service = mock(PatientService.class);
    private final PatientController controller = new PatientController(service);

    @Test
    void shouldDelegateToServiceAndReturnResult() {
        when(service.getPlaceholder()).thenReturn(Result.success("patient placeholder"));

        Result<String> result = controller.placeholder();
        assertEquals("SUCCESS", result.getCode());
        assertEquals("patient placeholder", result.getData());
    }
}
