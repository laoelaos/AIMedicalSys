package com.aimedical.modules.prescription.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.prescription.dto.assist.AiSuggestionResult;
import com.aimedical.modules.prescription.dto.assist.AiSuggestionStatus;
import com.aimedical.modules.prescription.dto.assist.DosageCheckRequest;
import com.aimedical.modules.prescription.dto.assist.DosageCheckResponse;
import com.aimedical.modules.prescription.dto.assist.PrescriptionAssistRequest;
import com.aimedical.modules.prescription.dto.assist.PrescriptionAssistResponse;
import com.aimedical.modules.prescription.service.assist.PrescriptionAssistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionAssistControllerTest {

    @Mock
    private PrescriptionAssistService prescriptionAssistService;

    private PrescriptionAssistController controller;

    @BeforeEach
    void setUp() {
        controller = new PrescriptionAssistController(prescriptionAssistService);
    }

    @Test
    void assistShouldReturn200() {
        when(prescriptionAssistService.assist(any())).thenReturn(new PrescriptionAssistResponse());

        Result<PrescriptionAssistResponse> result = controller.assist(new PrescriptionAssistRequest());

        assertTrue(result.getCode().equals("SUCCESS"));
        assertNotNull(result.getData());
    }

    @Test
    void checkDoseShouldReturn200() {
        when(prescriptionAssistService.checkDose(any())).thenReturn(new DosageCheckResponse());

        Result<DosageCheckResponse> result = controller.checkDose(new DosageCheckRequest());

        assertTrue(result.getCode().equals("SUCCESS"));
        assertNotNull(result.getData());
    }

    @Test
    void getSuggestionShouldReturn200WhenFound() {
        AiSuggestionResult suggestion = new AiSuggestionResult();
        suggestion.setTaskId("task-001");
        suggestion.setStatus(AiSuggestionStatus.COMPLETED);
        when(prescriptionAssistService.getSuggestion("task-001")).thenReturn(suggestion);

        ResponseEntity<Result<AiSuggestionResult>> result = controller.getSuggestion("task-001");

        assertEquals(HttpStatusCode.valueOf(200), result.getStatusCode());
        assertEquals("task-001", result.getBody().getData().getTaskId());
    }

    @Test
    void getSuggestionShouldReturnProcessing() {
        AiSuggestionResult suggestion = new AiSuggestionResult();
        suggestion.setTaskId("task-001");
        suggestion.setStatus(AiSuggestionStatus.PROCESSING);
        when(prescriptionAssistService.getSuggestion("task-001")).thenReturn(suggestion);

        ResponseEntity<Result<AiSuggestionResult>> result = controller.getSuggestion("task-001");

        assertEquals(HttpStatusCode.valueOf(200), result.getStatusCode());
        assertEquals(AiSuggestionStatus.PROCESSING, result.getBody().getData().getStatus());
    }

    @Test
    void getSuggestionShouldReturnTimeout() {
        AiSuggestionResult suggestion = new AiSuggestionResult();
        suggestion.setTaskId("task-001");
        suggestion.setStatus(AiSuggestionStatus.TIMEOUT);
        when(prescriptionAssistService.getSuggestion("task-001")).thenReturn(suggestion);

        ResponseEntity<Result<AiSuggestionResult>> result = controller.getSuggestion("task-001");

        assertEquals(HttpStatusCode.valueOf(200), result.getStatusCode());
        assertEquals(AiSuggestionStatus.TIMEOUT, result.getBody().getData().getStatus());
    }
}
