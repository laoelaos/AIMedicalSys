package com.aimedical.modules.prescription.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.prescription.dto.audit.AuditAlert;
import com.aimedical.modules.prescription.dto.audit.AuditRequest;
import com.aimedical.modules.prescription.dto.audit.AuditResponse;
import com.aimedical.modules.prescription.dto.audit.BlockResponse;
import com.aimedical.modules.prescription.dto.audit.SubmitRequest;
import com.aimedical.modules.prescription.dto.audit.SubmitResponse;
import com.aimedical.modules.prescription.service.audit.AuditRiskLevel;
import com.aimedical.modules.prescription.service.audit.PrescriptionAuditEnforcer;
import com.aimedical.modules.prescription.service.audit.PrescriptionAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionAuditControllerTest {

    @Mock
    private PrescriptionAuditService prescriptionAuditService;

    @Mock
    private PrescriptionAuditEnforcer prescriptionAuditEnforcer;

    private PrescriptionAuditController controller;

    @BeforeEach
    void setUp() {
        controller = new PrescriptionAuditController(prescriptionAuditService, prescriptionAuditEnforcer);
    }

    @Test
    void auditShouldReturn200WhenNotBlocked() {
        AuditResponse response = new AuditResponse();
        response.setRiskLevel(AuditRiskLevel.PASS);
        when(prescriptionAuditService.audit(any())).thenReturn(response);

        ResponseEntity<Result<AuditResponse>> result = controller.audit(new AuditRequest());

        assertEquals(HttpStatusCode.valueOf(200), result.getStatusCode());
        assertEquals("SUCCESS", result.getBody().getCode());
    }

    @Test
    void auditShouldReturn422WhenBlocked() {
        AuditResponse response = new AuditResponse();
        response.setRiskLevel(AuditRiskLevel.BLOCK);
        when(prescriptionAuditService.audit(any())).thenReturn(response);
        BlockResponse blockResponse = new BlockResponse(List.of("blocked"), "RX_AUDIT_BLOCKED", LocalDateTime.now());
        when(prescriptionAuditEnforcer.enforce(any(), any(), any()))
                .thenReturn(blockResponse);

        ResponseEntity<Result<AuditResponse>> result = controller.audit(new AuditRequest());

        assertEquals(HttpStatusCode.valueOf(422), result.getStatusCode());
        assertNotEquals("SUCCESS", result.getBody().getCode());
        assertEquals(blockResponse.getBlockCode(), result.getBody().getCode());
        assertTrue(result.getBody().getMessage().contains(blockResponse.getBlockReasons().get(0)));
    }

    @Test
    void auditShouldPassReasonsFromAlertsWhenBlocked() {
        AuditAlert alert1 = new AuditAlert();
        alert1.setAlertCode("D001");
        alert1.setAlertMessage("Dose exceeds limit");
        AuditAlert alert2 = new AuditAlert();
        alert2.setAlertCode("A001");
        alert2.setAlertMessage("Allergy found");

        AuditResponse response = new AuditResponse();
        response.setRiskLevel(AuditRiskLevel.BLOCK);
        response.setAlerts(List.of(alert1, alert2));
        when(prescriptionAuditService.audit(any())).thenReturn(response);

        BlockResponse blockResponse = new BlockResponse(
                List.of("Dose exceeds limit", "Allergy found"), "RX_AUDIT_BLOCKED", LocalDateTime.now());
        when(prescriptionAuditEnforcer.enforce(any(), any(), any()))
                .thenReturn(blockResponse);

        ResponseEntity<Result<AuditResponse>> result = controller.audit(new AuditRequest());

        verify(prescriptionAuditEnforcer).enforce(any(), argThat(reasons ->
                reasons.contains("Dose exceeds limit") && reasons.contains("Allergy found")), any());
    }

    @Test
    void auditShouldUseFallbackReasonWhenAlertsEmptyOnBlock() {
        AuditResponse response = new AuditResponse();
        response.setRiskLevel(AuditRiskLevel.BLOCK);
        response.setAlerts(new ArrayList<>());
        when(prescriptionAuditService.audit(any())).thenReturn(response);

        BlockResponse blockResponse = new BlockResponse(
                List.of("Prescription audit blocked"), "RX_AUDIT_BLOCKED", LocalDateTime.now());
        when(prescriptionAuditEnforcer.enforce(any(), any(), any()))
                .thenReturn(blockResponse);

        ResponseEntity<Result<AuditResponse>> result = controller.audit(new AuditRequest());

        verify(prescriptionAuditEnforcer).enforce(any(), argThat(reasons ->
                reasons.size() == 1 && "Prescription audit blocked".equals(reasons.get(0))), any());
    }

    @Test
    void submitShouldReturn200WhenSuccessful() {
        SubmitResponse response = new SubmitResponse();
        response.setSubmitted(true);
        response.setPrescriptionOrderId("order-001");
        when(prescriptionAuditService.submit(any())).thenReturn(response);

        ResponseEntity<Result<SubmitResponse>> result = controller.submit(new SubmitRequest());

        assertEquals(HttpStatusCode.valueOf(200), result.getStatusCode());
        assertEquals("SUCCESS", result.getBody().getCode());
    }

    @Test
    void submitShouldReturn422WhenBlocked() {
        SubmitResponse response = new SubmitResponse();
        response.setSubmitted(false);
        response.setBlockInfo(new BlockResponse(List.of("blocked"), "RX_BLOCK_CRITICAL_DOSE", LocalDateTime.now()));
        when(prescriptionAuditService.submit(any())).thenReturn(response);

        ResponseEntity<Result<SubmitResponse>> result = controller.submit(new SubmitRequest());

        assertEquals(HttpStatusCode.valueOf(422), result.getStatusCode());
        assertNotEquals("SUCCESS", result.getBody().getCode());
    }

    @Test
    void submitShouldReturn400WhenErrorCode() {
        SubmitResponse response = new SubmitResponse();
        response.setSubmitted(false);
        response.setErrorCode("RX_AUDIT_PRESCRIPTION_MODIFIED");
        when(prescriptionAuditService.submit(any())).thenReturn(response);

        ResponseEntity<Result<SubmitResponse>> result = controller.submit(new SubmitRequest());

        assertEquals(HttpStatusCode.valueOf(400), result.getStatusCode());
        assertNotEquals("SUCCESS", result.getBody().getCode());
    }

    @Test
    void revokeShouldReturn200() {
        doNothing().when(prescriptionAuditService).revoke(100L);

        ResponseEntity<Void> result = controller.revoke(100L);

        assertEquals(HttpStatusCode.valueOf(200), result.getStatusCode());
        verify(prescriptionAuditService).revoke(100L);
    }
}
