package com.aimedical.modules.medicalrecord.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.medicalrecord.dto.RecordGenerateRequest;
import com.aimedical.modules.medicalrecord.dto.RecordGenerateResponse;
import com.aimedical.modules.medicalrecord.exception.MedicalRecordErrorCode;
import com.aimedical.modules.medicalrecord.service.MedicalRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MedicalRecordControllerTest {

    private StubService stubService;
    private MedicalRecordController controller;

    @BeforeEach
    void setUp() {
        stubService = new StubService();
        controller = new MedicalRecordController(stubService);
    }

    @Test
    void shouldReturnFailWhenStreamIsTrue() {
        RecordGenerateRequest request = new RecordGenerateRequest();
        request.setStream(true);

        Result<RecordGenerateResponse> result = controller.generate(request);

        assertNotEquals("SUCCESS", result.getCode());
        assertEquals(MedicalRecordErrorCode.MR_GEN_STREAM_NOT_SUPPORTED.getCode(), result.getCode());
        assertFalse(stubService.generateCalled);
    }

    @Test
    void shouldReturnSuccessWhenResponseIsSuccess() {
        RecordGenerateRequest request = new RecordGenerateRequest();
        request.setStream(false);

        RecordGenerateResponse serviceResponse = new RecordGenerateResponse();
        serviceResponse.setSuccess(true);
        stubService.response = serviceResponse;

        Result<RecordGenerateResponse> result = controller.generate(request);

        assertEquals("SUCCESS", result.getCode());
        assertSame(serviceResponse, result.getData());
    }

    @Test
    void shouldReturnFailWhenResponseIsNotSuccess() {
        RecordGenerateRequest request = new RecordGenerateRequest();
        request.setStream(false);

        RecordGenerateResponse serviceResponse = new RecordGenerateResponse();
        serviceResponse.setSuccess(false);
        serviceResponse.setErrorCode(MedicalRecordErrorCode.MR_GEN_VISIT_NOT_FOUND);
        stubService.response = serviceResponse;

        Result<RecordGenerateResponse> result = controller.generate(request);

        assertNotEquals("SUCCESS", result.getCode());
        assertEquals(MedicalRecordErrorCode.MR_GEN_VISIT_NOT_FOUND.getCode(), result.getCode());
    }

    private static class StubService implements MedicalRecordService {
        RecordGenerateResponse response;
        boolean generateCalled;

        @Override
        public RecordGenerateResponse generate(RecordGenerateRequest request) {
            generateCalled = true;
            return response;
        }
    }
}
