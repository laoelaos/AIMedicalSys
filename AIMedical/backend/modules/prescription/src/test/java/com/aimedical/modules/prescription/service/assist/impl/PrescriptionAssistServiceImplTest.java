package com.aimedical.modules.prescription.service.assist.impl;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.modules.ai.api.AiResult;
import com.aimedical.modules.ai.api.AiService;
import com.aimedical.modules.commonmodule.drug.DrugFacade;
import com.aimedical.modules.commonmodule.store.SuggestionStore;
import com.aimedical.modules.prescription.PrescriptionErrorCode;
import com.aimedical.modules.prescription.context.PrescriptionDraftContext;
import com.aimedical.modules.prescription.converter.AssistConverter;
import com.aimedical.modules.prescription.dto.assist.*;
import com.aimedical.modules.prescription.dto.audit.PatientInfo;
import com.aimedical.modules.prescription.rule.AllergyCheckRule;
import com.aimedical.modules.prescription.rule.LocalRuleResult;
import com.aimedical.modules.prescription.service.assist.DedupTaskScheduler;
import com.aimedical.modules.prescription.service.assist.DosageThresholdService;
import com.aimedical.modules.prescription.service.audit.AuditRiskLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionAssistServiceImplTest {

    @Mock private AiService aiService;
    @Mock private AssistConverter assistConverter;
    @Mock private AllergyCheckRule allergyCheckRule;
    @Mock private DosageThresholdService dosageThresholdService;
    @Mock private PrescriptionDraftContext prescriptionDraftContext;
    @Mock private DedupTaskScheduler dedupTaskScheduler;
    @Mock private SuggestionStore suggestionStore;
    @Mock private DrugFacade drugFacade;
    @Mock private ExecutorService aiTaskExecutor;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PrescriptionAssistServiceImpl service;

    private PrescriptionAssistRequest assistRequest;
    private com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistRequest aiRequest;

    @BeforeEach
    void setUp() {
        service = new PrescriptionAssistServiceImpl(aiService, assistConverter, allergyCheckRule,
                dosageThresholdService, prescriptionDraftContext, dedupTaskScheduler, suggestionStore, objectMapper, 8L, drugFacade, aiTaskExecutor);

        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setPatientId("pat-001");
        patientInfo.setAge(30);

        assistRequest = new PrescriptionAssistRequest();
        assistRequest.setDiagnosis("感冒");
        assistRequest.setPatientInfo(patientInfo);

        aiRequest = new com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistRequest();
    }

    private com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse createAiResponseWithDrugs() {
        com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse resp =
                new com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse();
        resp.setPrescriptionDraft("{\"drugs\":[{\"drugId\":\"drug-001\",\"dose\":100,\"unit\":\"mg\",\"route\":\"oral\",\"frequency\":\"tid\"}]}");
        resp.setDoseWarnings(new ArrayList<>());
        resp.setAllergyWarnings(new ArrayList<>());
        resp.setDisclaimerRequired(true);
        return resp;
    }

    // ─── assist() tests ──────────────────────────────────────────────────────

    @Test
    void assistShouldGeneratePrescriptionIdWhenBlank() {
        aiRequest.setPrescriptionId(null);
        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> aiResult =
                AiResult.success(createAiResponseWithDrugs());
        when(aiService.prescriptionAssist(any())).thenReturn(CompletableFuture.completedFuture(aiResult));
        when(assistConverter.toPrescriptionAssistResponse(aiResult)).thenReturn(new PrescriptionAssistResponse());
        when(allergyCheckRule.check(any())).thenReturn(
            new LocalRuleResult("ALLERGY_CHECK", true, null, AuditRiskLevel.PASS));

        PrescriptionAssistResponse response = service.assist(assistRequest);

        assertNotNull(response);
        assertNotNull(assistRequest.getPrescriptionId());
    }

    @Test
    void assistShouldReturnEmptyOnTimeout() {
        CompletableFuture<AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse>> future =
                new CompletableFuture<>();
        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);
        when(aiService.prescriptionAssist(any())).thenReturn(future);

        PrescriptionAssistServiceImpl timeoutService = new PrescriptionAssistServiceImpl(
                aiService, assistConverter, allergyCheckRule, dosageThresholdService,
                prescriptionDraftContext, dedupTaskScheduler, suggestionStore, objectMapper, 0L, drugFacade, aiTaskExecutor);

        PrescriptionAssistResponse response = timeoutService.assist(assistRequest);

        assertEquals("{\"drugs\":[]}", response.getPrescriptionDraft());
        assertEquals(PrescriptionErrorCode.RX_ASSIST_AI_NO_RECOMMENDATION.getCode(), response.getErrorCode());
        verify(prescriptionDraftContext).updateCriticalAlerts(anyString(), eq(Collections.emptyList()));
    }

    @Test
    void assistShouldReturnEmptyWhenInterrupted() {
        CompletableFuture<AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse>> future =
                new CompletableFuture<>();
        future.completeExceptionally(new InterruptedException("interrupted"));
        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);
        when(aiService.prescriptionAssist(any())).thenReturn(future);

        PrescriptionAssistResponse response = service.assist(assistRequest);

        assertEquals("{\"drugs\":[]}", response.getPrescriptionDraft());
        assertEquals(PrescriptionErrorCode.RX_ASSIST_AI_NO_RECOMMENDATION.getCode(), response.getErrorCode());
        verify(prescriptionDraftContext).updateCriticalAlerts(anyString(), eq(Collections.emptyList()));
    }

    @Test
    void assistShouldReturnEmptyWhenExecutionException() {
        CompletableFuture<AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse>> future =
                new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("AI down"));
        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);
        when(aiService.prescriptionAssist(any())).thenReturn(future);

        PrescriptionAssistResponse response = service.assist(assistRequest);

        assertEquals("{\"drugs\":[]}", response.getPrescriptionDraft());
        assertEquals(PrescriptionErrorCode.RX_ASSIST_AI_NO_RECOMMENDATION.getCode(), response.getErrorCode());
        verify(prescriptionDraftContext).updateCriticalAlerts(anyString(), eq(Collections.emptyList()));
    }

    @Test
    void assistShouldReturnNoRecommendationWhenAiReturnsEmptyDrugs() throws Exception {
        com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse aiResp =
                new com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse();
        aiResp.setPrescriptionDraft("{\"drugs\":[]}");
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> aiResult = AiResult.success(aiResp);
        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);
        when(aiService.prescriptionAssist(any())).thenReturn(CompletableFuture.completedFuture(aiResult));

        PrescriptionAssistResponse response = service.assist(assistRequest);

        assertEquals(PrescriptionErrorCode.RX_ASSIST_AI_NO_RECOMMENDATION.getCode(), response.getErrorCode());
        assertEquals("{\"drugs\":[]}", response.getPrescriptionDraft());
        verify(prescriptionDraftContext).updateCriticalAlerts(anyString(), eq(Collections.emptyList()));
    }

    @Test
    void assistShouldClearCriticalAlertsWhenAiResultNotSuccess() {
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> aiResult =
                AiResult.failure("AI_ERR");
        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);
        when(aiService.prescriptionAssist(any())).thenReturn(CompletableFuture.completedFuture(aiResult));

        PrescriptionAssistResponse response = service.assist(assistRequest);

        assertEquals(PrescriptionErrorCode.RX_ASSIST_AI_NO_RECOMMENDATION.getCode(), response.getErrorCode());
        verify(prescriptionDraftContext).updateCriticalAlerts(anyString(), eq(Collections.emptyList()));
    }

    @Test
    void assistShouldReturnFullResponseWhenAiSuccessWithDrugs() throws Exception {
        com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse aiResp = createAiResponseWithDrugs();
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> aiResult = AiResult.success(aiResp);

        PrescriptionAssistResponse bizResponse = new PrescriptionAssistResponse();
        bizResponse.setPrescriptionDraft("{\"drugs\":[{\"drugId\":\"drug-001\"}]}");
        bizResponse.setDoseWarnings(new ArrayList<>());
        bizResponse.setAllergyWarnings(new ArrayList<>());
        bizResponse.setDisclaimerRequired(true);

        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);
        when(aiService.prescriptionAssist(any())).thenReturn(CompletableFuture.completedFuture(aiResult));
        when(assistConverter.toPrescriptionAssistResponse(aiResult)).thenReturn(bizResponse);
        when(allergyCheckRule.check(any())).thenReturn(new LocalRuleResult("ALLERGY_CHECK", true, null, AuditRiskLevel.PASS));
        when(dosageThresholdService.check(any())).thenReturn(new ArrayList<>());

        PrescriptionAssistResponse response = service.assist(assistRequest);

        assertNotNull(response);
        verify(prescriptionDraftContext).updateCriticalAlerts(anyString(), anyList());
    }

    @Test
    void assistShouldMergeLocalDoseWarningsIntoResponse() {
        com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse aiResp = createAiResponseWithDrugs();
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> aiResult = AiResult.success(aiResp);

        PrescriptionAssistResponse bizResponse = new PrescriptionAssistResponse();
        bizResponse.setPrescriptionDraft("{\"drugs\":[{\"drugId\":\"drug-001\"}]}");
        bizResponse.setDoseWarnings(new ArrayList<>());
        bizResponse.setAllergyWarnings(new ArrayList<>());
        bizResponse.setDisclaimerRequired(true);

        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);
        when(aiService.prescriptionAssist(any())).thenReturn(CompletableFuture.completedFuture(aiResult));
        when(assistConverter.toPrescriptionAssistResponse(aiResult)).thenReturn(bizResponse);
        when(allergyCheckRule.check(any())).thenReturn(new LocalRuleResult("ALLERGY_CHECK", true, null, AuditRiskLevel.PASS));

        DosageAlert localAlert = new DosageAlert();
        localAlert.setAlertLevel(DosageAlertLevel.WARNING);
        localAlert.setWarningType(DoseWarningType.OVER_SINGLE_DOSE);
        localAlert.setDrugCode("drug-001");
        localAlert.setCurrentDose(150);
        localAlert.setMessage("单次剂量超过上限");
        when(dosageThresholdService.check(any())).thenReturn(List.of(localAlert));

        PrescriptionAssistResponse response = service.assist(assistRequest);

        assertEquals(1, response.getDoseWarnings().size());
        assertEquals("drug-001", response.getDoseWarnings().get(0).getDrugId());
        assertEquals(DoseWarningType.OVER_SINGLE_DOSE, response.getDoseWarnings().get(0).getWarningType());
        assertEquals(DosageAlertLevel.WARNING, response.getDoseWarnings().get(0).getSeverity());
        assertEquals("单次剂量超过上限", response.getDoseWarnings().get(0).getMessage());
    }

    @Test
    void assistShouldIncludeAllergyWarnings() throws Exception {
        com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse aiResp = createAiResponseWithDrugs();
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> aiResult = AiResult.success(aiResp);

        PrescriptionAssistResponse bizResponse = new PrescriptionAssistResponse();
        bizResponse.setPrescriptionDraft("{\"drugs\":[{\"drugId\":\"drug-001\"}]}");
        bizResponse.setDoseWarnings(new ArrayList<>());
        bizResponse.setAllergyWarnings(new ArrayList<>());
        bizResponse.setDisclaimerRequired(true);

        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);
        when(aiService.prescriptionAssist(any())).thenReturn(CompletableFuture.completedFuture(aiResult));
        when(assistConverter.toPrescriptionAssistResponse(aiResult)).thenReturn(bizResponse);
        when(allergyCheckRule.check(any()))
                .thenReturn(new LocalRuleResult("ALLERGY_CHECK", false, "Severe allergy to 青霉素 for drug drug-001", AuditRiskLevel.BLOCK));
        when(dosageThresholdService.check(any())).thenReturn(new ArrayList<>());

        PrescriptionAssistResponse response = service.assist(assistRequest);

        assertEquals(1, response.getAllergyWarnings().size());
        assertEquals(AllergyWarningSeverity.HIGH, response.getAllergyWarnings().get(0).getSeverity());
    }

    // ─── checkDose() tests ───────────────────────────────────────────────────

    @Test
    void checkDoseShouldReturnFullResponse() {
        DosageCheckRequest doseReq = new DosageCheckRequest();
        doseReq.setDrugCode("drug-001");
        doseReq.setDosage(100);
        doseReq.setUnit("mg");
        doseReq.setRouteOfAdministration("oral");

        DosageAlert alert = new DosageAlert();
        alert.setAlertLevel(DosageAlertLevel.WARNING);
        alert.setDrugCode("drug-001");

        when(dosageThresholdService.check(any())).thenReturn(List.of(alert));
        when(dedupTaskScheduler.schedule(anyString())).thenReturn("task-001");
        when(prescriptionDraftContext.getContextCriticalCount(anyString())).thenReturn(0);

        DosageCheckResponse response = service.checkDose(doseReq);

        assertEquals(1, response.getAlerts().size());
        assertEquals("task-001", response.getTaskId());
        assertEquals(0, response.getContextCriticalCount());
        assertNotNull(response.getPrescriptionId());
    }

    @Test
    void checkDoseShouldGeneratePrescriptionIdWhenBlank() {
        DosageCheckRequest doseReq = new DosageCheckRequest();
        doseReq.setDrugCode("drug-001");
        doseReq.setDosage(100);
        doseReq.setUnit("mg");
        doseReq.setRouteOfAdministration("oral");

        when(dosageThresholdService.check(any())).thenReturn(new ArrayList<>());
        when(dedupTaskScheduler.schedule(anyString())).thenReturn("task-001");
        when(prescriptionDraftContext.getContextCriticalCount(anyString())).thenReturn(0);

        DosageCheckResponse response = service.checkDose(doseReq);

        assertNotNull(doseReq.getPrescriptionId());
        assertNotNull(response.getPrescriptionId());
    }

    @Test
    void checkDoseShouldWriteCriticalAlertsToContext() {
        DosageCheckRequest doseReq = new DosageCheckRequest();
        doseReq.setPrescriptionId("rx-001");
        doseReq.setDrugCode("drug-001");
        doseReq.setDosage(500);
        doseReq.setUnit("mg");
        doseReq.setRouteOfAdministration("oral");

        DosageAlert critical = new DosageAlert();
        critical.setAlertLevel(DosageAlertLevel.CRITICAL);
        critical.setDrugCode("drug-001");
        critical.setMessage("严重超量");

        when(dosageThresholdService.check(any())).thenReturn(List.of(critical));
        when(dedupTaskScheduler.schedule("rx-001")).thenReturn("task-001");
        when(prescriptionDraftContext.getContextCriticalCount("rx-001")).thenReturn(1);

        DosageCheckResponse response = service.checkDose(doseReq);

        assertEquals(1, response.getContextCriticalCount());
        verify(prescriptionDraftContext).updateCriticalAlerts(eq("rx-001"), anyList());
    }

    // ─── getSuggestion() tests ───────────────────────────────────────────────

    @Test
    void getSuggestionShouldThrowNotFoundWhenNotExists() {
        when(suggestionStore.get("unknown-task")).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.getSuggestion("unknown-task"));
    }

    @Test
    void getSuggestionShouldSetConsumedWhenCompleted() {
        AiSuggestionResult result = new AiSuggestionResult();
        result.setTaskId("task-001");
        result.setStatus(AiSuggestionStatus.COMPLETED);
        result.setConsumed(false);
        result.setSuggestion("{\"drugs\":[]}");

        when(suggestionStore.get("task-001")).thenReturn(result);

        AiSuggestionResult returned = service.getSuggestion("task-001");

        assertTrue(returned.isConsumed());
        verify(suggestionStore).put("task-001", result);
    }

    @Test
    void getSuggestionShouldReturnPendingDirectly() {
        AiSuggestionResult result = new AiSuggestionResult();
        result.setTaskId("task-001");
        result.setStatus(AiSuggestionStatus.PENDING);

        when(suggestionStore.get("task-001")).thenReturn(result);

        AiSuggestionResult returned = service.getSuggestion("task-001");

        assertEquals(AiSuggestionStatus.PENDING, returned.getStatus());
        verify(suggestionStore, never()).put(anyString(), any());
    }

    @Test
    void getSuggestionShouldReturnFailedDirectly() {
        AiSuggestionResult result = new AiSuggestionResult();
        result.setTaskId("task-001");
        result.setStatus(AiSuggestionStatus.FAILED);
        result.setFailReason("AI timeout");

        when(suggestionStore.get("task-001")).thenReturn(result);

        AiSuggestionResult returned = service.getSuggestion("task-001");

        assertEquals(AiSuggestionStatus.FAILED, returned.getStatus());
        assertEquals("AI timeout", returned.getFailReason());
        verify(suggestionStore, never()).put(anyString(), any());
    }

    @Test
    void getSuggestionShouldReturnProcessingDirectly() {
        AiSuggestionResult result = new AiSuggestionResult();
        result.setTaskId("task-001");
        result.setStatus(AiSuggestionStatus.PROCESSING);

        when(suggestionStore.get("task-001")).thenReturn(result);

        AiSuggestionResult returned = service.getSuggestion("task-001");

        assertEquals(AiSuggestionStatus.PROCESSING, returned.getStatus());
        verify(suggestionStore, never()).put(anyString(), any());
    }

    @Test
    void getSuggestionShouldReturnTimeoutDirectly() {
        AiSuggestionResult result = new AiSuggestionResult();
        result.setTaskId("task-001");
        result.setStatus(AiSuggestionStatus.TIMEOUT);

        when(suggestionStore.get("task-001")).thenReturn(result);

        AiSuggestionResult returned = service.getSuggestion("task-001");

        assertEquals(AiSuggestionStatus.TIMEOUT, returned.getStatus());
        verify(suggestionStore, never()).put(anyString(), any());
    }

    // ─── async suggestion pipeline tests ────────────────────────────────────

    @Test
    void assistShouldTriggerAsyncSchedulingWhenSyncAiSucceeds() {
        aiRequest.setPrescriptionId("rx-001");
        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);

        var aiResp = createAiResponseWithDrugs();
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> aiResult = AiResult.success(aiResp);
        when(aiService.prescriptionAssist(any())).thenReturn(CompletableFuture.completedFuture(aiResult));

        PrescriptionAssistResponse bizResponse = new PrescriptionAssistResponse();
        bizResponse.setPrescriptionDraft("{\"drugs\":[{\"drugId\":\"drug-001\"}]}");
        bizResponse.setDoseWarnings(new ArrayList<>());
        bizResponse.setAllergyWarnings(new ArrayList<>());
        bizResponse.setDisclaimerRequired(true);
        when(assistConverter.toPrescriptionAssistResponse(aiResult)).thenReturn(bizResponse);
        when(allergyCheckRule.check(any())).thenReturn(new LocalRuleResult("ALLERGY_CHECK", true, null, AuditRiskLevel.PASS));
        when(dosageThresholdService.check(any())).thenReturn(new ArrayList<>());

        String taskId = "async-task-001";
        when(dedupTaskScheduler.schedule(anyString())).thenReturn(taskId);

        PrescriptionAssistResponse response = service.assist(assistRequest);

        assertNotNull(response);
        verify(dedupTaskScheduler).schedule(anyString());
    }

    @Test
    void assistShouldReturnWithoutWaitingForAsyncSuggestion() {
        aiRequest.setPrescriptionId("rx-001");
        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);

        var aiResp = createAiResponseWithDrugs();
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> syncAiResult = AiResult.success(aiResp);
        CompletableFuture<AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse>> pendingFuture = new CompletableFuture<>();

        when(aiService.prescriptionAssist(any()))
                .thenReturn(CompletableFuture.completedFuture(syncAiResult))
                .thenReturn(pendingFuture);

        PrescriptionAssistResponse bizResponse = new PrescriptionAssistResponse();
        bizResponse.setPrescriptionDraft("{\"drugs\":[{\"drugId\":\"drug-001\"}]}");
        bizResponse.setDoseWarnings(new ArrayList<>());
        bizResponse.setAllergyWarnings(new ArrayList<>());
        bizResponse.setDisclaimerRequired(true);
        when(assistConverter.toPrescriptionAssistResponse(syncAiResult)).thenReturn(bizResponse);
        when(allergyCheckRule.check(any())).thenReturn(new LocalRuleResult("ALLERGY_CHECK", true, null, AuditRiskLevel.PASS));
        when(dosageThresholdService.check(any())).thenReturn(new ArrayList<>());

        when(dedupTaskScheduler.schedule(anyString())).thenReturn("async-task-001");

        long start = System.currentTimeMillis();
        PrescriptionAssistResponse response = service.assist(assistRequest);
        long elapsed = System.currentTimeMillis() - start;

        assertNotNull(response);
        assertTrue(elapsed < 1000, "assist() should return synchronously without waiting for async pipeline");
    }

    @Test
    void asyncSuggestionShouldStoreCompletedOnAiSuccess() throws Exception {
        aiRequest.setPrescriptionId("rx-001");
        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);

        var aiResp = createAiResponseWithDrugs();
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> aiResult = AiResult.success(aiResp);
        when(aiService.prescriptionAssist(any())).thenReturn(CompletableFuture.completedFuture(aiResult));

        PrescriptionAssistResponse bizResponse = new PrescriptionAssistResponse();
        bizResponse.setPrescriptionDraft("{\"drugs\":[{\"drugId\":\"drug-001\"}]}");
        bizResponse.setDoseWarnings(new ArrayList<>());
        bizResponse.setAllergyWarnings(new ArrayList<>());
        bizResponse.setDisclaimerRequired(true);
        when(assistConverter.toPrescriptionAssistResponse(aiResult)).thenReturn(bizResponse);
        when(allergyCheckRule.check(any())).thenReturn(new LocalRuleResult("ALLERGY_CHECK", true, null, AuditRiskLevel.PASS));
        when(dosageThresholdService.check(any())).thenReturn(new ArrayList<>());

        String taskId = "async-task-001";
        when(dedupTaskScheduler.schedule(anyString())).thenReturn(taskId);

        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return null;
        }).when(aiTaskExecutor).execute(any(Runnable.class));

        service.assist(assistRequest);

        Thread.sleep(300);

        verify(suggestionStore).put(eq(taskId), argThat((AiSuggestionResult result) ->
                result.getStatus() == AiSuggestionStatus.COMPLETED
        ));
    }

    @Test
    void asyncSuggestionShouldStoreCompletedWithSerializedSuggestion() throws Exception {
        aiRequest.setPrescriptionId("rx-001");
        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);

        var aiResp = createAiResponseWithDrugs();
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> aiResult = AiResult.success(aiResp);
        when(aiService.prescriptionAssist(any())).thenReturn(CompletableFuture.completedFuture(aiResult));

        PrescriptionAssistResponse bizResponse = new PrescriptionAssistResponse();
        bizResponse.setPrescriptionDraft("{\"drugs\":[{\"drugId\":\"drug-001\"}]}");
        bizResponse.setDoseWarnings(new ArrayList<>());
        bizResponse.setAllergyWarnings(new ArrayList<>());
        bizResponse.setDisclaimerRequired(true);
        when(assistConverter.toPrescriptionAssistResponse(aiResult)).thenReturn(bizResponse);
        when(allergyCheckRule.check(any())).thenReturn(new LocalRuleResult("ALLERGY_CHECK", true, null, AuditRiskLevel.PASS));
        when(dosageThresholdService.check(any())).thenReturn(new ArrayList<>());

        String taskId = "async-task-001";
        when(dedupTaskScheduler.schedule(anyString())).thenReturn(taskId);

        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return null;
        }).when(aiTaskExecutor).execute(any(Runnable.class));

        service.assist(assistRequest);

        Thread.sleep(300);

        ArgumentCaptor<AiSuggestionResult> captor = ArgumentCaptor.forClass(AiSuggestionResult.class);
        verify(suggestionStore, times(2)).put(eq(taskId), captor.capture());
        AiSuggestionResult stored = captor.getValue();

        assertEquals(AiSuggestionStatus.COMPLETED, stored.getStatus());
        assertNotNull(stored.getSuggestion());
        assertTrue(stored.getSuggestion().contains("drugs"));
    }

    @Test
    void asyncSuggestionShouldStoreFailedWhenAsyncAiThrows() throws Exception {
        aiRequest.setPrescriptionId("rx-001");
        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);

        var aiResp = createAiResponseWithDrugs();
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> syncAiResult = AiResult.success(aiResp);
        CompletableFuture<AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse>> asyncFuture = new CompletableFuture<>();
        asyncFuture.completeExceptionally(new RuntimeException("AI service timeout"));

        when(aiService.prescriptionAssist(any()))
                .thenReturn(CompletableFuture.completedFuture(syncAiResult))
                .thenReturn(asyncFuture);

        PrescriptionAssistResponse bizResponse = new PrescriptionAssistResponse();
        bizResponse.setPrescriptionDraft("{\"drugs\":[{\"drugId\":\"drug-001\"}]}");
        bizResponse.setDoseWarnings(new ArrayList<>());
        bizResponse.setAllergyWarnings(new ArrayList<>());
        bizResponse.setDisclaimerRequired(true);
        when(assistConverter.toPrescriptionAssistResponse(syncAiResult)).thenReturn(bizResponse);
        when(allergyCheckRule.check(any())).thenReturn(new LocalRuleResult("ALLERGY_CHECK", true, null, AuditRiskLevel.PASS));
        when(dosageThresholdService.check(any())).thenReturn(new ArrayList<>());

        String taskId = "async-task-001";
        when(dedupTaskScheduler.schedule(anyString())).thenReturn(taskId);

        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return null;
        }).when(aiTaskExecutor).execute(any(Runnable.class));

        service.assist(assistRequest);

        Thread.sleep(300);

        verify(suggestionStore).put(eq(taskId), argThat((AiSuggestionResult result) ->
                result.getStatus() == AiSuggestionStatus.FAILED
                        && result.getFailReason() != null
                        && result.getFailReason().contains("RuntimeException")
        ));
    }

    @Test
    void asyncSuggestionShouldStoreFailedWithTruncatedReason() throws Exception {
        aiRequest.setPrescriptionId("rx-001");
        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);

        var aiResp = createAiResponseWithDrugs();
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> syncAiResult = AiResult.success(aiResp);
        String longMsg = "x".repeat(500);
        CompletableFuture<AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse>> asyncFuture = new CompletableFuture<>();
        asyncFuture.completeExceptionally(new RuntimeException(longMsg));

        when(aiService.prescriptionAssist(any()))
                .thenReturn(CompletableFuture.completedFuture(syncAiResult))
                .thenReturn(asyncFuture);

        PrescriptionAssistResponse bizResponse = new PrescriptionAssistResponse();
        bizResponse.setPrescriptionDraft("{\"drugs\":[{\"drugId\":\"drug-001\"}]}");
        bizResponse.setDoseWarnings(new ArrayList<>());
        bizResponse.setAllergyWarnings(new ArrayList<>());
        bizResponse.setDisclaimerRequired(true);
        when(assistConverter.toPrescriptionAssistResponse(syncAiResult)).thenReturn(bizResponse);
        when(allergyCheckRule.check(any())).thenReturn(new LocalRuleResult("ALLERGY_CHECK", true, null, AuditRiskLevel.PASS));
        when(dosageThresholdService.check(any())).thenReturn(new ArrayList<>());

        String taskId = "async-task-001";
        when(dedupTaskScheduler.schedule(anyString())).thenReturn(taskId);

        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return null;
        }).when(aiTaskExecutor).execute(any(Runnable.class));

        service.assist(assistRequest);

        Thread.sleep(300);

        ArgumentCaptor<AiSuggestionResult> captor = ArgumentCaptor.forClass(AiSuggestionResult.class);
        verify(suggestionStore, times(2)).put(eq(taskId), captor.capture());
        AiSuggestionResult stored = captor.getValue();

        assertEquals(AiSuggestionStatus.FAILED, stored.getStatus());
        assertNotNull(stored.getFailReason());
        assertTrue(stored.getFailReason().length() <= "java.util.concurrent.ExecutionException: ".length() + 200);
    }

    @Test
    void asyncSuggestionShouldStoreFailedOnTimeoutException() throws Exception {
        aiRequest.setPrescriptionId("rx-001");
        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);

        var aiResp = createAiResponseWithDrugs();
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> syncAiResult = AiResult.success(aiResp);
        CompletableFuture<AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse>> asyncFuture = new CompletableFuture<>();
        asyncFuture.completeExceptionally(new java.util.concurrent.TimeoutException("timed out"));

        when(aiService.prescriptionAssist(any()))
                .thenReturn(CompletableFuture.completedFuture(syncAiResult))
                .thenReturn(asyncFuture);

        PrescriptionAssistResponse bizResponse = new PrescriptionAssistResponse();
        bizResponse.setPrescriptionDraft("{\"drugs\":[{\"drugId\":\"drug-001\"}]}");
        bizResponse.setDoseWarnings(new ArrayList<>());
        bizResponse.setAllergyWarnings(new ArrayList<>());
        bizResponse.setDisclaimerRequired(true);
        when(assistConverter.toPrescriptionAssistResponse(syncAiResult)).thenReturn(bizResponse);
        when(allergyCheckRule.check(any())).thenReturn(new LocalRuleResult("ALLERGY_CHECK", true, null, AuditRiskLevel.PASS));
        when(dosageThresholdService.check(any())).thenReturn(new ArrayList<>());

        String taskId = "async-task-001";
        when(dedupTaskScheduler.schedule(anyString())).thenReturn(taskId);

        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return null;
        }).when(aiTaskExecutor).execute(any(Runnable.class));

        service.assist(assistRequest);

        Thread.sleep(300);

        verify(suggestionStore).put(eq(taskId), argThat((AiSuggestionResult result) ->
                result.getStatus() == AiSuggestionStatus.FAILED
                        && result.getFailReason() != null
                        && result.getFailReason().contains("ExecutionException")
        ));
    }

    @Test
    void asyncSuggestionShouldStoreFailedWhenAiResultNotSuccess() throws Exception {
        aiRequest.setPrescriptionId("rx-001");
        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);

        var aiResp = createAiResponseWithDrugs();
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> syncAiResult = AiResult.success(aiResp);
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> asyncAiResult = AiResult.failure("AI_ERR");

        when(aiService.prescriptionAssist(any()))
                .thenReturn(CompletableFuture.completedFuture(syncAiResult))
                .thenReturn(CompletableFuture.completedFuture(asyncAiResult));

        PrescriptionAssistResponse bizResponse = new PrescriptionAssistResponse();
        bizResponse.setPrescriptionDraft("{\"drugs\":[{\"drugId\":\"drug-001\"}]}");
        bizResponse.setDoseWarnings(new ArrayList<>());
        bizResponse.setAllergyWarnings(new ArrayList<>());
        bizResponse.setDisclaimerRequired(true);
        when(assistConverter.toPrescriptionAssistResponse(syncAiResult)).thenReturn(bizResponse);
        when(allergyCheckRule.check(any())).thenReturn(new LocalRuleResult("ALLERGY_CHECK", true, null, AuditRiskLevel.PASS));
        when(dosageThresholdService.check(any())).thenReturn(new ArrayList<>());

        String taskId = "async-task-001";
        when(dedupTaskScheduler.schedule(anyString())).thenReturn(taskId);

        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return null;
        }).when(aiTaskExecutor).execute(any(Runnable.class));

        service.assist(assistRequest);

        Thread.sleep(300);

        verify(suggestionStore).put(eq(taskId), argThat((AiSuggestionResult result) ->
                result.getStatus() == AiSuggestionStatus.FAILED
                        && "AI result not successful or data is null".equals(result.getFailReason())
        ));
    }

    @Test
    void asyncSuggestionShouldStoreFailedWhenAiResultDataIsNull() throws Exception {
        aiRequest.setPrescriptionId("rx-001");
        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);

        var aiResp = createAiResponseWithDrugs();
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> syncAiResult = AiResult.success(aiResp);
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> asyncAiResult =
                new AiResult<>(true, null, null, false, null);

        when(aiService.prescriptionAssist(any()))
                .thenReturn(CompletableFuture.completedFuture(syncAiResult))
                .thenReturn(CompletableFuture.completedFuture(asyncAiResult));

        PrescriptionAssistResponse bizResponse = new PrescriptionAssistResponse();
        bizResponse.setPrescriptionDraft("{\"drugs\":[{\"drugId\":\"drug-001\"}]}");
        bizResponse.setDoseWarnings(new ArrayList<>());
        bizResponse.setAllergyWarnings(new ArrayList<>());
        bizResponse.setDisclaimerRequired(true);
        when(assistConverter.toPrescriptionAssistResponse(syncAiResult)).thenReturn(bizResponse);
        when(allergyCheckRule.check(any())).thenReturn(new LocalRuleResult("ALLERGY_CHECK", true, null, AuditRiskLevel.PASS));
        when(dosageThresholdService.check(any())).thenReturn(new ArrayList<>());

        String taskId = "async-task-001";
        when(dedupTaskScheduler.schedule(anyString())).thenReturn(taskId);

        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return null;
        }).when(aiTaskExecutor).execute(any(Runnable.class));

        service.assist(assistRequest);

        Thread.sleep(300);

        verify(suggestionStore).put(eq(taskId), argThat((AiSuggestionResult result) ->
                result.getStatus() == AiSuggestionStatus.FAILED
                        && "AI result not successful or data is null".equals(result.getFailReason())
        ));
    }

    @SuppressWarnings("unchecked")
    @Test
    void asyncSuggestionShouldStoreTimeoutOnTimeoutException() throws Exception {
        aiRequest.setPrescriptionId("rx-001");
        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);

        var aiResp = createAiResponseWithDrugs();
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> syncAiResult = AiResult.success(aiResp);

        CompletableFuture<AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse>> asyncFuture =
                mock(CompletableFuture.class);
        when(asyncFuture.get(anyLong(), any(TimeUnit.class))).thenThrow(new java.util.concurrent.TimeoutException("timed out"));

        when(aiService.prescriptionAssist(any()))
                .thenReturn(CompletableFuture.completedFuture(syncAiResult))
                .thenReturn(asyncFuture);

        PrescriptionAssistResponse bizResponse = new PrescriptionAssistResponse();
        bizResponse.setPrescriptionDraft("{\"drugs\":[{\"drugId\":\"drug-001\"}]}");
        bizResponse.setDoseWarnings(new ArrayList<>());
        bizResponse.setAllergyWarnings(new ArrayList<>());
        bizResponse.setDisclaimerRequired(true);
        when(assistConverter.toPrescriptionAssistResponse(syncAiResult)).thenReturn(bizResponse);
        when(allergyCheckRule.check(any())).thenReturn(new LocalRuleResult("ALLERGY_CHECK", true, null, AuditRiskLevel.PASS));
        when(dosageThresholdService.check(any())).thenReturn(new ArrayList<>());

        String taskId = "async-task-001";
        when(dedupTaskScheduler.schedule(anyString())).thenReturn(taskId);

        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return null;
        }).when(aiTaskExecutor).execute(any(Runnable.class));

        service.assist(assistRequest);

        Thread.sleep(300);

        verify(suggestionStore).put(eq(taskId), argThat((AiSuggestionResult result) ->
                result.getStatus() == AiSuggestionStatus.TIMEOUT
                        && result.getFailReason() != null
                        && result.getFailReason().contains("TimeoutException")
        ));
    }

    @SuppressWarnings("unchecked")
    @Test
    void asyncSuggestionShouldStoreFailedOnInterruptedException() throws Exception {
        aiRequest.setPrescriptionId("rx-001");
        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);

        var aiResp = createAiResponseWithDrugs();
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> syncAiResult = AiResult.success(aiResp);

        CompletableFuture<AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse>> asyncFuture =
                mock(CompletableFuture.class);
        when(asyncFuture.get(anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException("interrupted"));

        when(aiService.prescriptionAssist(any()))
                .thenReturn(CompletableFuture.completedFuture(syncAiResult))
                .thenReturn(asyncFuture);

        PrescriptionAssistResponse bizResponse = new PrescriptionAssistResponse();
        bizResponse.setPrescriptionDraft("{\"drugs\":[{\"drugId\":\"drug-001\"}]}");
        bizResponse.setDoseWarnings(new ArrayList<>());
        bizResponse.setAllergyWarnings(new ArrayList<>());
        bizResponse.setDisclaimerRequired(true);
        when(assistConverter.toPrescriptionAssistResponse(syncAiResult)).thenReturn(bizResponse);
        when(allergyCheckRule.check(any())).thenReturn(new LocalRuleResult("ALLERGY_CHECK", true, null, AuditRiskLevel.PASS));
        when(dosageThresholdService.check(any())).thenReturn(new ArrayList<>());

        String taskId = "async-task-001";
        when(dedupTaskScheduler.schedule(anyString())).thenReturn(taskId);

        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return null;
        }).when(aiTaskExecutor).execute(any(Runnable.class));

        service.assist(assistRequest);
        Thread.interrupted();

        Thread.sleep(300);

        verify(suggestionStore).put(eq(taskId), argThat((AiSuggestionResult result) ->
                result.getStatus() == AiSuggestionStatus.FAILED
                        && result.getFailReason() != null
                        && result.getFailReason().contains("InterruptedException")
        ));
    }

    @Test
    void assistShouldClearCriticalAlertsIdempotently() {
        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> aiResult =
                AiResult.failure("AI_ERR");
        when(aiService.prescriptionAssist(any())).thenReturn(CompletableFuture.completedFuture(aiResult));

        service.assist(assistRequest);
        service.assist(assistRequest);

        verify(prescriptionDraftContext, times(2)).updateCriticalAlerts(anyString(), eq(Collections.emptyList()));
    }

    @Test
    void clearCriticalAlertsShouldPassCorrectPrescriptionIdOnTimeout() {
        assistRequest.setPrescriptionId("rx-explicit-id");
        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);

        CompletableFuture<AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse>> future =
                new CompletableFuture<>();
        when(aiService.prescriptionAssist(any())).thenReturn(future);

        PrescriptionAssistServiceImpl timeoutService = new PrescriptionAssistServiceImpl(
                aiService, assistConverter, allergyCheckRule, dosageThresholdService,
                prescriptionDraftContext, dedupTaskScheduler, suggestionStore, objectMapper, 0L, drugFacade, aiTaskExecutor);

        timeoutService.assist(assistRequest);

        verify(prescriptionDraftContext).updateCriticalAlerts(eq("rx-explicit-id"), eq(Collections.emptyList()));
    }

    @Test
    void assistShouldNotCallClearCriticalAlertsOnNormalPath() {
        com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse aiResp = createAiResponseWithDrugs();
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> aiResult = AiResult.success(aiResp);

        PrescriptionAssistResponse bizResponse = new PrescriptionAssistResponse();
        bizResponse.setPrescriptionDraft("{\"drugs\":[{\"drugId\":\"drug-001\"}]}");
        bizResponse.setDoseWarnings(new ArrayList<>());
        bizResponse.setAllergyWarnings(new ArrayList<>());
        bizResponse.setDisclaimerRequired(true);

        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);
        when(aiService.prescriptionAssist(any())).thenReturn(CompletableFuture.completedFuture(aiResult));
        when(assistConverter.toPrescriptionAssistResponse(aiResult)).thenReturn(bizResponse);
        when(allergyCheckRule.check(any())).thenReturn(new LocalRuleResult("ALLERGY_CHECK", true, null, AuditRiskLevel.PASS));
        when(dedupTaskScheduler.schedule(anyString())).thenReturn("test-task-id");

        DosageAlert criticalAlert = new DosageAlert();
        criticalAlert.setAlertLevel(DosageAlertLevel.CRITICAL);
        criticalAlert.setDrugCode("drug-001");
        criticalAlert.setMessage("严重超量");
        when(dosageThresholdService.check(any())).thenReturn(List.of(criticalAlert));

        service.assist(assistRequest);

        ArgumentCaptor<List<com.aimedical.modules.prescription.context.DosageAlert>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(prescriptionDraftContext).updateCriticalAlerts(anyString(), captor.capture());
        List<com.aimedical.modules.prescription.context.DosageAlert> captured = captor.getValue();
        assertFalse(captured.isEmpty(), "Normal path should write non-empty CRITICAL alerts");
    }

    @Test
    void constructorShouldAcceptNineParameters() throws Exception {
        var constructor = PrescriptionAssistServiceImpl.class.getConstructors();
        assertEquals(1, constructor.length);
        assertEquals(11, constructor[0].getParameterCount());
    }

    @Test
    void assistShouldClearCriticalAlertsWhenAiReturnsNullData() {
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> aiResult =
                new AiResult<>(true, null, null, false, null);
        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);
        when(aiService.prescriptionAssist(any())).thenReturn(CompletableFuture.completedFuture(aiResult));
        when(dedupTaskScheduler.schedule(anyString())).thenReturn("test-task-id");

        PrescriptionAssistResponse response = service.assist(assistRequest);

        assertEquals(PrescriptionErrorCode.RX_ASSIST_AI_NO_RECOMMENDATION.getCode(), response.getErrorCode());
        verify(prescriptionDraftContext).updateCriticalAlerts(anyString(), eq(Collections.emptyList()));
    }

    @Test
    void asyncSuggestionShouldStoreFailedWhenSerializationFails() throws Exception {
        aiRequest.setPrescriptionId("rx-001");
        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);

        var aiResp = createAiResponseWithDrugs();
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> syncAiResult = AiResult.success(aiResp);
        when(aiService.prescriptionAssist(any())).thenReturn(CompletableFuture.completedFuture(syncAiResult));

        PrescriptionAssistResponse bizResponse = new PrescriptionAssistResponse();
        bizResponse.setPrescriptionDraft("{\"drugs\":[{\"drugId\":\"drug-001\"}]}");
        bizResponse.setDoseWarnings(new ArrayList<>());
        bizResponse.setAllergyWarnings(new ArrayList<>());
        bizResponse.setDisclaimerRequired(true);
        when(assistConverter.toPrescriptionAssistResponse(syncAiResult)).thenReturn(bizResponse);
        when(allergyCheckRule.check(any())).thenReturn(new LocalRuleResult("ALLERGY_CHECK", true, null, AuditRiskLevel.PASS));
        when(dosageThresholdService.check(any())).thenReturn(new ArrayList<>());

        String taskId = "async-task-001";
        when(dedupTaskScheduler.schedule(anyString())).thenReturn(taskId);

        ObjectMapper failingMapper = spy(new ObjectMapper());
        doThrow(new RuntimeException("serialization failed")).when(failingMapper).writeValueAsString(any());

        PrescriptionAssistServiceImpl testService = new PrescriptionAssistServiceImpl(
                aiService, assistConverter, allergyCheckRule, dosageThresholdService,
                prescriptionDraftContext, dedupTaskScheduler, suggestionStore, failingMapper, 8L, drugFacade, aiTaskExecutor);

        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return null;
        }).when(aiTaskExecutor).execute(any(Runnable.class));

        testService.assist(assistRequest);

        Thread.sleep(300);

        verify(suggestionStore).put(eq(taskId), argThat((AiSuggestionResult result) ->
                result.getStatus() == AiSuggestionStatus.FAILED
                        && result.getFailReason() != null
                        && result.getFailReason().contains("serialization failed")
        ));
    }

    @Test
    void asyncSuggestionShouldClearCriticalAlertsOnExceptionally() throws Exception {
        aiRequest.setPrescriptionId("rx-001");
        when(assistConverter.toAiPrescriptionAssistRequest(any())).thenReturn(aiRequest);

        var aiResp = createAiResponseWithDrugs();
        AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse> syncAiResult = AiResult.success(aiResp);
        when(aiService.prescriptionAssist(any()))
                .thenReturn(CompletableFuture.completedFuture(syncAiResult));

        PrescriptionAssistResponse bizResponse = new PrescriptionAssistResponse();
        bizResponse.setPrescriptionDraft("{\"drugs\":[{\"drugId\":\"drug-001\"}]}");
        bizResponse.setDoseWarnings(new ArrayList<>());
        bizResponse.setAllergyWarnings(new ArrayList<>());
        bizResponse.setDisclaimerRequired(true);
        when(assistConverter.toPrescriptionAssistResponse(syncAiResult)).thenReturn(bizResponse);
        when(allergyCheckRule.check(any())).thenReturn(new LocalRuleResult("ALLERGY_CHECK", true, null, AuditRiskLevel.PASS));
        when(dosageThresholdService.check(any())).thenReturn(new ArrayList<>());

        String taskId = "async-task-exceptionally";
        when(dedupTaskScheduler.schedule(anyString())).thenReturn(taskId);

        doThrow(new RuntimeException("store error")).when(suggestionStore).put(eq(taskId), any());

        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return null;
        }).when(aiTaskExecutor).execute(any(Runnable.class));

        assistRequest.setPrescriptionId("rx-001");
        service.assist(assistRequest);

        Thread.sleep(300);

        verify(prescriptionDraftContext, times(2)).updateCriticalAlerts(eq("rx-001"), eq(Collections.emptyList()));
        verify(dedupTaskScheduler).schedule(anyString());
    }
}
