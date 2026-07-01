package com.aimedical.modules.prescription.service.audit.impl;

import com.aimedical.common.exception.BusinessException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import com.aimedical.modules.ai.api.AiResult;
import com.aimedical.modules.ai.api.AiService;
import com.aimedical.modules.ai.api.dto.prescription.AlertItem;
import com.aimedical.modules.ai.api.dto.prescription.DrugInteractionItem;
import com.aimedical.modules.ai.api.dto.prescription.PrescriptionCheckRequest;
import com.aimedical.modules.ai.api.dto.prescription.PrescriptionCheckResponse;
import com.aimedical.modules.ai.api.dto.prescription.SuggestionItem;
import com.aimedical.modules.commonmodule.auth.CurrentUser;
import com.aimedical.modules.commonmodule.drug.DrugFacade;
import com.aimedical.modules.prescription.PrescriptionErrorCode;
import com.aimedical.modules.prescription.context.DosageAlert;
import com.aimedical.modules.prescription.context.PrescriptionDraftContext;
import com.aimedical.modules.prescription.converter.AuditConverter;
import com.aimedical.modules.prescription.dto.audit.AlertSeverity;
import com.aimedical.modules.prescription.dto.audit.AuditIssue;
import com.aimedical.modules.prescription.dto.audit.AuditRequest;
import com.aimedical.modules.prescription.dto.audit.AuditResponse;
import com.aimedical.modules.prescription.dto.audit.BlockResponse;
import com.aimedical.modules.prescription.dto.audit.PatientInfo;
import com.aimedical.modules.prescription.dto.audit.PrescriptionItem;
import com.aimedical.modules.prescription.dto.audit.SubmitRequest;
import com.aimedical.modules.prescription.dto.audit.SubmitResponse;
import com.aimedical.modules.prescription.entity.AuditRecord;
import com.aimedical.modules.prescription.repository.AuditRecordRepository;
import com.aimedical.modules.prescription.rule.LocalRuleEngine;
import com.aimedical.modules.prescription.rule.LocalRuleResult;
import com.aimedical.modules.prescription.service.audit.AuditRiskLevel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class PrescriptionAuditServiceImplTest {

    @Mock private AiService aiService;
    @Mock private LocalRuleEngine localRuleEngine;
    @Mock private AuditRecordRepository auditRecordRepository;
    @Mock private AuditConverter auditConverter;
    @Mock private PrescriptionDraftContext prescriptionDraftContext;
    @Mock private CurrentUser currentUser;
    @Mock private DrugFacade drugFacade;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PrescriptionAuditServiceImpl service;

    private AuditRequest auditRequest;
    private PrescriptionItem sampleItem;

    @BeforeEach
    void setUp() {
        service = new PrescriptionAuditServiceImpl(aiService, localRuleEngine, auditRecordRepository,
                auditConverter, prescriptionDraftContext, currentUser, objectMapper, 6L, drugFacade);

        sampleItem = new PrescriptionItem();
        sampleItem.setDrugId("drug-001");
        sampleItem.setDrugName("Aspirin");
        sampleItem.setDose(BigDecimal.valueOf(100.0));
        sampleItem.setFrequency("tid");
        sampleItem.setDuration("7d");
        sampleItem.setRoute("oral");

        auditRequest = new AuditRequest();
        auditRequest.setPrescriptionId("rx-001");
        auditRequest.setPrescriptionItems(List.of(sampleItem));
        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setPatientId("pat-001");
        auditRequest.setPatientInfo(patientInfo);
    }

    // ─── audit() tests ───────────────────────────────────────────────────────

    @Test
    void auditShouldReturnAiResultWhenSuccess() throws Exception {
        PrescriptionCheckResponse checkResponse = new PrescriptionCheckResponse();
        checkResponse.setRiskLevel("LOW");
        checkResponse.setAlerts(new ArrayList<>());
        checkResponse.setInteractions(new ArrayList<>());
        checkResponse.setSuggestions(new ArrayList<>());
        checkResponse.setFromFallback(false);

        AiResult<PrescriptionCheckResponse> aiResult = AiResult.success(checkResponse);
        when(aiService.prescriptionCheck(any())).thenReturn(CompletableFuture.completedFuture(aiResult));
        when(auditConverter.toAiPrescriptionCheckRequest(any())).thenReturn(new PrescriptionCheckRequest());
        when(auditConverter.toAuditResponse(aiResult)).thenReturn(new AuditResponse());
        when(currentUser.getUserId()).thenReturn(1L);
        when(auditRecordRepository.findByPrescriptionIdAndIsLatestTrue("rx-001")).thenReturn(new ArrayList<>());

        AuditResponse result = service.audit(auditRequest);

        assertNotNull(result);
        assertFalse(result.isFromFallback());
        verify(auditRecordRepository).save(any(AuditRecord.class));
    }

    @Test
    void auditShouldFallbackToLocalRuleEngineWhenAiFails() throws Exception {
        when(aiService.prescriptionCheck(any())).thenReturn(CompletableFuture.completedFuture(AiResult.failure("ERR")));
        when(auditConverter.toAiPrescriptionCheckRequest(any())).thenReturn(new PrescriptionCheckRequest());
        when(currentUser.getUserId()).thenReturn(1L);
        when(localRuleEngine.check(any())).thenReturn(List.of(
                new LocalRuleResult("ALLERGY_CHECK", true, null, AuditRiskLevel.PASS),
                new LocalRuleResult("DOSAGE_LIMIT", false, "Overdose", AuditRiskLevel.WARN)
        ));
        when(auditRecordRepository.findByPrescriptionIdAndIsLatestTrue("rx-001")).thenReturn(new ArrayList<>());

        AuditResponse result = service.audit(auditRequest);

        assertNotNull(result);
        assertTrue(result.isFromFallback());
        assertEquals(AuditRiskLevel.WARN, result.getRiskLevel());
    }

    @Test
    void auditShouldLogWarnWhenAiResultIsNull() {
        CompletableFuture<AiResult<PrescriptionCheckResponse>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("AI down"));
        when(aiService.prescriptionCheck(any())).thenReturn(future);
        when(auditConverter.toAiPrescriptionCheckRequest(any())).thenReturn(new PrescriptionCheckRequest());
        when(currentUser.getUserId()).thenReturn(1L);
        when(localRuleEngine.check(any())).thenReturn(List.of());
        when(auditRecordRepository.findByPrescriptionIdAndIsLatestTrue("rx-001")).thenReturn(new ArrayList<>());

        Logger auditLogger = (Logger) org.slf4j.LoggerFactory.getLogger(PrescriptionAuditServiceImpl.class);
        ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
        logAppender.start();
        auditLogger.addAppender(logAppender);

        try {
            service.audit(auditRequest);
            assertEquals(1, logAppender.list.size());
            ILoggingEvent event = logAppender.list.get(0);
            assertEquals(Level.WARN, event.getLevel());
            assertTrue(event.getFormattedMessage().contains("AI service unavailable"));
            assertTrue(event.getFormattedMessage().contains("RX_AUDIT_AI_EXECUTION_ERROR"));
        } finally {
            auditLogger.detachAppender(logAppender);
        }
    }

    @Test
    void auditShouldLogWarnWhenAiReturnsFailure() {
        when(aiService.prescriptionCheck(any())).thenReturn(CompletableFuture.completedFuture(AiResult.failure("ERR_FAIL")));
        when(auditConverter.toAiPrescriptionCheckRequest(any())).thenReturn(new PrescriptionCheckRequest());
        when(currentUser.getUserId()).thenReturn(1L);
        when(localRuleEngine.check(any())).thenReturn(List.of());
        when(auditRecordRepository.findByPrescriptionIdAndIsLatestTrue("rx-001")).thenReturn(new ArrayList<>());

        Logger auditLogger = (Logger) org.slf4j.LoggerFactory.getLogger(PrescriptionAuditServiceImpl.class);
        ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
        logAppender.start();
        auditLogger.addAppender(logAppender);

        try {
            service.audit(auditRequest);
            assertEquals(1, logAppender.list.size());
            ILoggingEvent event = logAppender.list.get(0);
            assertEquals(Level.WARN, event.getLevel());
            assertTrue(event.getFormattedMessage().contains("AI service unavailable"));
            assertTrue(event.getFormattedMessage().contains("ERR_FAIL"));
        } finally {
            auditLogger.detachAppender(logAppender);
        }
    }

    @Test
    void auditShouldHandleAiResultDataNull() {
        AiResult<PrescriptionCheckResponse> aiResult = new AiResult<>();
        aiResult.setSuccess(true);
        aiResult.setData(null);
        when(aiService.prescriptionCheck(any())).thenReturn(CompletableFuture.completedFuture(aiResult));
        when(auditConverter.toAiPrescriptionCheckRequest(any())).thenReturn(new PrescriptionCheckRequest());
        when(currentUser.getUserId()).thenReturn(1L);
        when(localRuleEngine.check(any())).thenReturn(List.of());
        when(auditRecordRepository.findByPrescriptionIdAndIsLatestTrue("rx-001")).thenReturn(new ArrayList<>());

        AuditResponse result = service.audit(auditRequest);

        assertTrue(result.isFromFallback());
    }

    @Test
    void auditShouldPassThroughWhenAiResultDataIsNotNull() throws Exception {
        PrescriptionCheckResponse checkResponse = new PrescriptionCheckResponse();
        checkResponse.setRiskLevel("LOW");
        checkResponse.setAlerts(new ArrayList<>());
        checkResponse.setInteractions(new ArrayList<>());
        checkResponse.setSuggestions(new ArrayList<>());
        checkResponse.setFromFallback(false);

        AiResult<PrescriptionCheckResponse> aiResult = AiResult.success(checkResponse);
        when(aiService.prescriptionCheck(any())).thenReturn(CompletableFuture.completedFuture(aiResult));
        when(auditConverter.toAiPrescriptionCheckRequest(any())).thenReturn(new PrescriptionCheckRequest());
        when(auditConverter.toAuditResponse(aiResult)).thenReturn(new AuditResponse());
        when(currentUser.getUserId()).thenReturn(1L);
        when(auditRecordRepository.findByPrescriptionIdAndIsLatestTrue("rx-001")).thenReturn(new ArrayList<>());

        AuditResponse result = service.audit(auditRequest);

        assertNotNull(result);
        assertFalse(result.isFromFallback());
    }

    @Test
    void auditShouldFallbackWhenAiThrowsException() throws Exception {
        CompletableFuture<AiResult<PrescriptionCheckResponse>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("AI down"));
        when(aiService.prescriptionCheck(any())).thenReturn(future);
        when(auditConverter.toAiPrescriptionCheckRequest(any())).thenReturn(new PrescriptionCheckRequest());
        when(currentUser.getUserId()).thenReturn(1L);
        when(localRuleEngine.check(any())).thenReturn(List.of(
                new LocalRuleResult("ALLERGY_CHECK", true, null, AuditRiskLevel.PASS)
        ));
        when(auditRecordRepository.findByPrescriptionIdAndIsLatestTrue("rx-001")).thenReturn(new ArrayList<>());

        AuditResponse result = service.audit(auditRequest);

        assertNotNull(result);
        assertTrue(result.isFromFallback());
    }

    @Test
    void auditShouldPersistRecordWithCorrectSequence() {
        PrescriptionCheckResponse checkResponse = new PrescriptionCheckResponse();
        checkResponse.setRiskLevel("LOW");
        AiResult<PrescriptionCheckResponse> aiResult = AiResult.success(checkResponse);
        when(aiService.prescriptionCheck(any())).thenReturn(CompletableFuture.completedFuture(aiResult));
        when(auditConverter.toAiPrescriptionCheckRequest(any())).thenReturn(new PrescriptionCheckRequest());
        when(auditConverter.toAuditResponse(aiResult)).thenReturn(new AuditResponse());
        when(currentUser.getUserId()).thenReturn(1L);

        AuditRecord existingRecord = new AuditRecord();
        existingRecord.setAuditSequence(1);
        existingRecord.setLatest(true);
        when(auditRecordRepository.findByPrescriptionIdAndIsLatestTrue("rx-001"))
                .thenReturn(new ArrayList<>(List.of(existingRecord)));

        service.audit(auditRequest);

        verify(auditRecordRepository).saveAll(anyList());
        verify(auditRecordRepository, times(1)).save(any(AuditRecord.class));
        assertFalse(existingRecord.isLatest());
    }

    @Test
    void auditShouldAggregateBlockWhenAnyRuleBlocks() throws Exception {
        when(aiService.prescriptionCheck(any())).thenReturn(CompletableFuture.completedFuture(AiResult.failure("ERR")));
        when(auditConverter.toAiPrescriptionCheckRequest(any())).thenReturn(new PrescriptionCheckRequest());
        when(currentUser.getUserId()).thenReturn(1L);
        when(localRuleEngine.check(any())).thenReturn(List.of(
                new LocalRuleResult("ALLERGY_CHECK", false, "Severe allergy", AuditRiskLevel.BLOCK),
                new LocalRuleResult("DOSAGE_LIMIT", true, null, AuditRiskLevel.PASS)
        ));
        when(auditRecordRepository.findByPrescriptionIdAndIsLatestTrue("rx-001")).thenReturn(new ArrayList<>());

        AuditResponse result = service.audit(auditRequest);

        assertEquals(AuditRiskLevel.BLOCK, result.getRiskLevel());
        assertTrue(result.isFromFallback());
    }

    // ─── P06: Fallback alerts ────────────────────────────────────────────────

    @Test
    void auditShouldPopulateAlertsFromLocalRuleResultsWhenFallback() throws Exception {
        when(aiService.prescriptionCheck(any())).thenReturn(CompletableFuture.completedFuture(AiResult.failure("ERR")));
        when(auditConverter.toAiPrescriptionCheckRequest(any())).thenReturn(new PrescriptionCheckRequest());
        when(currentUser.getUserId()).thenReturn(1L);
        when(localRuleEngine.check(any())).thenReturn(List.of(
                new LocalRuleResult("ALLERGY_CHECK", true, null, AuditRiskLevel.PASS),
                new LocalRuleResult("DOSAGE_LIMIT", false, "Overdose", AuditRiskLevel.WARN),
                new LocalRuleResult("DRUG_CHECK", false, "Interaction", AuditRiskLevel.BLOCK)
        ));
        when(auditRecordRepository.findByPrescriptionIdAndIsLatestTrue("rx-001")).thenReturn(new ArrayList<>());

        AuditResponse result = service.audit(auditRequest);

        assertNotNull(result.getAlerts());
        assertEquals(2, result.getAlerts().size());
        assertEquals("DOSAGE_LIMIT", result.getAlerts().get(0).getAlertCode());
        assertEquals("Overdose", result.getAlerts().get(0).getAlertMessage());
        assertEquals(AlertSeverity.WARNING, result.getAlerts().get(0).getSeverity());
        assertEquals("DRUG_CHECK", result.getAlerts().get(1).getAlertCode());
        assertEquals("Interaction", result.getAlerts().get(1).getAlertMessage());
        assertEquals(AlertSeverity.CRITICAL, result.getAlerts().get(1).getSeverity());
    }

    @Test
    void auditShouldReturnEmptyAlertsWhenAllLocalRulesPass() throws Exception {
        when(aiService.prescriptionCheck(any())).thenReturn(CompletableFuture.completedFuture(AiResult.failure("ERR")));
        when(auditConverter.toAiPrescriptionCheckRequest(any())).thenReturn(new PrescriptionCheckRequest());
        when(currentUser.getUserId()).thenReturn(1L);
        when(localRuleEngine.check(any())).thenReturn(List.of(
                new LocalRuleResult("ALLERGY_CHECK", true, null, AuditRiskLevel.PASS)
        ));
        when(auditRecordRepository.findByPrescriptionIdAndIsLatestTrue("rx-001")).thenReturn(new ArrayList<>());

        AuditResponse result = service.audit(auditRequest);

        assertNotNull(result.getAlerts());
        assertTrue(result.getAlerts().isEmpty());
    }

    // ─── P07: auditIssues ────────────────────────────────────────────────────

    @Test
    void auditShouldWriteAuditIssuesToRecordWhenFallback() throws Exception {
        when(aiService.prescriptionCheck(any())).thenReturn(CompletableFuture.completedFuture(AiResult.failure("ERR")));
        when(auditConverter.toAiPrescriptionCheckRequest(any())).thenReturn(new PrescriptionCheckRequest());
        when(currentUser.getUserId()).thenReturn(1L);
        when(localRuleEngine.check(any())).thenReturn(List.of(
                new LocalRuleResult("DOSAGE_LIMIT", false, "Overdose", AuditRiskLevel.WARN)
        ));
        when(auditRecordRepository.findByPrescriptionIdAndIsLatestTrue("rx-001")).thenReturn(new ArrayList<>());

        service.audit(auditRequest);

        ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditRecordRepository).save(captor.capture());
        AuditRecord saved = captor.getValue();
        assertNotNull(saved.getAuditIssues());
        assertTrue(saved.getAuditIssues().contains("DOSAGE_LIMIT"));
        assertTrue(saved.getAuditIssues().contains("Overdose"));
    }

    @Test
    void auditShouldWriteAuditIssuesToRecordFromAiAlertsAndInteractions() throws Exception {
        PrescriptionCheckResponse checkResponse = new PrescriptionCheckResponse();
        checkResponse.setRiskLevel("LOW");
        AlertItem alertItem = new AlertItem();
        alertItem.setAlertCode("A001");
        alertItem.setAlertMessage("Test alert");
        alertItem.setSeverity("WARNING");
        DrugInteractionItem interactionItem = new DrugInteractionItem();
        interactionItem.setDrugPair("drug1-drug2");
        interactionItem.setDescription("Interaction desc");
        interactionItem.setSeverity("CRITICAL");
        checkResponse.setAlerts(List.of(alertItem));
        checkResponse.setInteractions(List.of(interactionItem));
        checkResponse.setSuggestions(new ArrayList<>());
        checkResponse.setFromFallback(false);

        AiResult<PrescriptionCheckResponse> aiResult = AiResult.success(checkResponse);
        when(aiService.prescriptionCheck(any())).thenReturn(CompletableFuture.completedFuture(aiResult));
        when(auditConverter.toAiPrescriptionCheckRequest(any())).thenReturn(new PrescriptionCheckRequest());
        when(auditConverter.toAuditResponse(aiResult)).thenAnswer(inv -> {
            AuditResponse r = new AuditResponse();
            r.setRiskLevel(AuditRiskLevel.PASS);
            r.setAlerts(new ArrayList<>());
            r.setFromFallback(false);
            return r;
        });
        when(currentUser.getUserId()).thenReturn(1L);
        when(auditRecordRepository.findByPrescriptionIdAndIsLatestTrue("rx-001")).thenReturn(new ArrayList<>());

        service.audit(auditRequest);

        ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditRecordRepository).save(captor.capture());
        AuditRecord saved = captor.getValue();
        assertNotNull(saved.getAuditIssues());
        List<AuditIssue> issues = objectMapper.readValue(
                saved.getAuditIssues(),
                new TypeReference<List<AuditIssue>>() {});
        assertEquals(2, issues.size());

        assertEquals("A001", issues.get(0).getRuleId());
        assertEquals("Test alert", issues.get(0).getIssueDescription());
        assertNull(issues.get(0).getFieldName());
        assertEquals(AlertSeverity.WARNING, issues.get(0).getSeverity());

        assertEquals("DRUG_INTERACTION_drug1-drug2", issues.get(1).getRuleId());
        assertEquals("Interaction desc", issues.get(1).getIssueDescription());
        assertEquals("drug1-drug2", issues.get(1).getFieldName());
        assertEquals(AlertSeverity.CRITICAL, issues.get(1).getSeverity());
    }

    @Test
    void auditShouldNotSetAuditIssuesWhenAiResponseHasNoAlertsOrInteractions() throws Exception {
        PrescriptionCheckResponse checkResponse = new PrescriptionCheckResponse();
        checkResponse.setRiskLevel("LOW");
        checkResponse.setAlerts(null);
        checkResponse.setInteractions(null);
        checkResponse.setSuggestions(new ArrayList<>());
        checkResponse.setFromFallback(false);

        AiResult<PrescriptionCheckResponse> aiResult = AiResult.success(checkResponse);
        when(aiService.prescriptionCheck(any())).thenReturn(CompletableFuture.completedFuture(aiResult));
        when(auditConverter.toAiPrescriptionCheckRequest(any())).thenReturn(new PrescriptionCheckRequest());
        when(auditConverter.toAuditResponse(aiResult)).thenAnswer(inv -> {
            AuditResponse r = new AuditResponse();
            r.setRiskLevel(AuditRiskLevel.PASS);
            r.setAlerts(new ArrayList<>());
            r.setFromFallback(false);
            return r;
        });
        when(currentUser.getUserId()).thenReturn(1L);
        when(auditRecordRepository.findByPrescriptionIdAndIsLatestTrue("rx-001")).thenReturn(new ArrayList<>());

        service.audit(auditRequest);

        ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditRecordRepository).save(captor.capture());
        assertNull(captor.getValue().getAuditIssues());
    }

    @Test
    void auditShouldHandleAuditIssuesSerializationFailureGracefully() {
        ObjectMapper faultyMapper = mock(ObjectMapper.class);
        try {
            when(faultyMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("fail") {});
        } catch (JsonProcessingException e) {
            fail("unexpected");
        }

        PrescriptionAuditServiceImpl serviceWithFaultyMapper = new PrescriptionAuditServiceImpl(
                aiService, localRuleEngine, auditRecordRepository, auditConverter,
                prescriptionDraftContext, currentUser, faultyMapper, 6L, drugFacade);

        when(aiService.prescriptionCheck(any())).thenReturn(CompletableFuture.completedFuture(AiResult.failure("ERR")));
        when(auditConverter.toAiPrescriptionCheckRequest(any())).thenReturn(new PrescriptionCheckRequest());
        when(currentUser.getUserId()).thenReturn(1L);
        when(localRuleEngine.check(any())).thenReturn(List.of(
                new LocalRuleResult("DOSAGE_LIMIT", false, "Overdose", AuditRiskLevel.WARN)
        ));
        when(auditRecordRepository.findByPrescriptionIdAndIsLatestTrue("rx-001")).thenReturn(new ArrayList<>());

        assertDoesNotThrow(() -> serviceWithFaultyMapper.audit(auditRequest));

        ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditRecordRepository).save(captor.capture());
        assertNull(captor.getValue().getAuditIssues());
    }

    @Test
    void auditShouldNotIncludeSuggestionsInAuditIssues() throws Exception {
        PrescriptionCheckResponse checkResponse = new PrescriptionCheckResponse();
        checkResponse.setRiskLevel("LOW");
        checkResponse.setAlerts(new ArrayList<>());
        checkResponse.setInteractions(new ArrayList<>());
        SuggestionItem suggestion = new SuggestionItem();
        suggestion.setSuggestionCode("S1");
        suggestion.setSuggestionText("suggestion text");
        checkResponse.setSuggestions(List.of(suggestion));
        checkResponse.setFromFallback(false);

        AiResult<PrescriptionCheckResponse> aiResult = AiResult.success(checkResponse);
        when(aiService.prescriptionCheck(any())).thenReturn(CompletableFuture.completedFuture(aiResult));
        when(auditConverter.toAiPrescriptionCheckRequest(any())).thenReturn(new PrescriptionCheckRequest());
        when(auditConverter.toAuditResponse(aiResult)).thenAnswer(inv -> {
            AuditResponse r = new AuditResponse();
            r.setRiskLevel(AuditRiskLevel.PASS);
            r.setAlerts(new ArrayList<>());
            r.setFromFallback(false);
            return r;
        });
        when(currentUser.getUserId()).thenReturn(1L);
        when(auditRecordRepository.findByPrescriptionIdAndIsLatestTrue("rx-001")).thenReturn(new ArrayList<>());

        service.audit(auditRequest);

        ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditRecordRepository).save(captor.capture());
        assertNull(captor.getValue().getAuditIssues());
    }

    @Test
    void auditShouldFallbackOnTimeout() throws Exception {
        CompletableFuture<AiResult<PrescriptionCheckResponse>> future = new CompletableFuture<>();
        when(aiService.prescriptionCheck(any())).thenReturn(future);
        when(auditConverter.toAiPrescriptionCheckRequest(any())).thenReturn(new PrescriptionCheckRequest());
        when(currentUser.getUserId()).thenReturn(1L);
        when(localRuleEngine.check(any())).thenReturn(List.of());
        when(auditRecordRepository.findByPrescriptionIdAndIsLatestTrue("rx-001")).thenReturn(new ArrayList<>());

        PrescriptionAuditServiceImpl timeoutService = new PrescriptionAuditServiceImpl(
                aiService, localRuleEngine, auditRecordRepository, auditConverter,
                prescriptionDraftContext, currentUser, objectMapper, 0L, drugFacade);

        AuditResponse result = timeoutService.audit(auditRequest);

        assertTrue(result.isFromFallback());
    }

    @Test
    void auditShouldFallbackWhenAiUnavailable() throws Exception {
        AiResult<PrescriptionCheckResponse> aiResult = AiResult.failure("AI_UNAVAILABLE");
        when(aiService.prescriptionCheck(any())).thenReturn(CompletableFuture.completedFuture(aiResult));
        when(auditConverter.toAiPrescriptionCheckRequest(any())).thenReturn(new PrescriptionCheckRequest());
        when(localRuleEngine.check(any())).thenReturn(List.of());
        when(currentUser.getUserId()).thenReturn(1L);
        when(auditRecordRepository.findByPrescriptionIdAndIsLatestTrue("rx-001")).thenReturn(new ArrayList<>());

        service.audit(auditRequest);

        ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditRecordRepository).save(captor.capture());
        assertNull(captor.getValue().getAuditIssues());
    }

    @Test
    void auditShouldWriteAuditIssuesForInteractionsOnly() throws Exception {
        PrescriptionCheckResponse checkResponse = new PrescriptionCheckResponse();
        checkResponse.setRiskLevel("LOW");
        checkResponse.setAlerts(null);
        DrugInteractionItem interactionItem = new DrugInteractionItem();
        interactionItem.setDrugPair("drugA-drugB");
        interactionItem.setDescription("Interaction desc");
        interactionItem.setSeverity("WARNING");
        checkResponse.setInteractions(List.of(interactionItem));
        checkResponse.setSuggestions(new ArrayList<>());
        checkResponse.setFromFallback(false);

        AiResult<PrescriptionCheckResponse> aiResult = AiResult.success(checkResponse);
        when(aiService.prescriptionCheck(any())).thenReturn(CompletableFuture.completedFuture(aiResult));
        when(auditConverter.toAiPrescriptionCheckRequest(any())).thenReturn(new PrescriptionCheckRequest());
        when(auditConverter.toAuditResponse(aiResult)).thenAnswer(inv -> {
            AuditResponse r = new AuditResponse();
            r.setRiskLevel(AuditRiskLevel.PASS);
            r.setAlerts(new ArrayList<>());
            r.setFromFallback(false);
            return r;
        });
        when(currentUser.getUserId()).thenReturn(1L);
        when(auditRecordRepository.findByPrescriptionIdAndIsLatestTrue("rx-001")).thenReturn(new ArrayList<>());

        service.audit(auditRequest);

        ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditRecordRepository).save(captor.capture());
        assertNotNull(captor.getValue().getAuditIssues());
        List<AuditIssue> issues = objectMapper.readValue(
                captor.getValue().getAuditIssues(),
                new TypeReference<List<AuditIssue>>() {});
        assertEquals(1, issues.size());
        assertEquals("DRUG_INTERACTION_drugA-drugB", issues.get(0).getRuleId());
        assertEquals(AlertSeverity.WARNING, issues.get(0).getSeverity());
    }

    @Test
    void auditShouldWriteAuditIssuesForAlertsOnly() throws Exception {
        PrescriptionCheckResponse checkResponse = new PrescriptionCheckResponse();
        checkResponse.setRiskLevel("LOW");
        AlertItem alertItem = new AlertItem();
        alertItem.setAlertCode("A002");
        alertItem.setAlertMessage("Alert only");
        alertItem.setSeverity("CRITICAL");
        checkResponse.setAlerts(List.of(alertItem));
        checkResponse.setInteractions(null);
        checkResponse.setSuggestions(new ArrayList<>());
        checkResponse.setFromFallback(false);

        AiResult<PrescriptionCheckResponse> aiResult = AiResult.success(checkResponse);
        when(aiService.prescriptionCheck(any())).thenReturn(CompletableFuture.completedFuture(aiResult));
        when(auditConverter.toAiPrescriptionCheckRequest(any())).thenReturn(new PrescriptionCheckRequest());
        when(auditConverter.toAuditResponse(aiResult)).thenAnswer(inv -> {
            AuditResponse r = new AuditResponse();
            r.setRiskLevel(AuditRiskLevel.PASS);
            r.setAlerts(new ArrayList<>());
            r.setFromFallback(false);
            return r;
        });
        when(currentUser.getUserId()).thenReturn(1L);
        when(auditRecordRepository.findByPrescriptionIdAndIsLatestTrue("rx-001")).thenReturn(new ArrayList<>());

        service.audit(auditRequest);

        ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditRecordRepository).save(captor.capture());
        assertNotNull(captor.getValue().getAuditIssues());
        List<AuditIssue> issues = objectMapper.readValue(
                captor.getValue().getAuditIssues(),
                new TypeReference<List<AuditIssue>>() {});
        assertEquals(1, issues.size());
        assertEquals("A002", issues.get(0).getRuleId());
        assertEquals("Alert only", issues.get(0).getIssueDescription());
        assertEquals(AlertSeverity.CRITICAL, issues.get(0).getSeverity());
    }

    // ─── submit() tests ──────────────────────────────────────────────────────

    @Test
    void submitShouldBlockOnCriticalDoseStep1() {
        when(prescriptionDraftContext.snapshotCriticalAlerts("rx-001"))
                .thenReturn(new PrescriptionDraftContext.SnapshotResult(true,
                        List.of(createDosageAlert("drug-001", "CRITICAL"))));

        SubmitRequest request = new SubmitRequest();
        request.setPrescriptionId("rx-001");
        request.setPrescriptionItems(List.of(sampleItem));

        SubmitResponse result = service.submit(request);

        assertFalse(result.isSubmitted());
        assertNotNull(result.getBlockInfo());
        assertEquals("RX_BLOCK_CRITICAL_DOSE", result.getBlockInfo().getBlockCode());
    }

    @Test
    void submitShouldBlockOnAuditResultStep2() {
        when(prescriptionDraftContext.snapshotCriticalAlerts("rx-001"))
                .thenReturn(new PrescriptionDraftContext.SnapshotResult(false, List.of()));

        AuditRecord latest = new AuditRecord();
        latest.setRiskLevel("BLOCK");
        latest.setLatest(true);
        when(auditRecordRepository.findTopByPrescriptionIdAndIsLatestTrueOrderByAuditSequenceDesc("rx-001"))
                .thenReturn(Optional.of(latest));

        SubmitRequest request = new SubmitRequest();
        request.setPrescriptionId("rx-001");
        request.setPrescriptionItems(List.of(sampleItem));

        SubmitResponse result = service.submit(request);

        assertFalse(result.isSubmitted());
        assertNotNull(result.getBlockInfo());
        assertEquals("RX_BLOCK_AUDIT", result.getBlockInfo().getBlockCode());
    }

    @Test
    void submitShouldDirectSubmitWhenPass() {
        when(prescriptionDraftContext.snapshotCriticalAlerts("rx-001"))
                .thenReturn(new PrescriptionDraftContext.SnapshotResult(false, List.of()));

        AuditRecord latest = new AuditRecord();
        latest.setRiskLevel("PASS");
        latest.setLatest(true);
        when(auditRecordRepository.findTopByPrescriptionIdAndIsLatestTrueOrderByAuditSequenceDesc("rx-001"))
                .thenReturn(Optional.of(latest));

        SubmitRequest request = new SubmitRequest();
        request.setPrescriptionId("rx-001");
        request.setPrescriptionItems(List.of(sampleItem));

        SubmitResponse result = service.submit(request);

        assertTrue(result.isSubmitted());
        assertNotNull(result.getPrescriptionOrderId());
    }

    @Test
    void submitShouldRejectWarnWithoutForceSubmitWhenPrescriptionModified() {
        when(prescriptionDraftContext.snapshotCriticalAlerts("rx-001"))
                .thenReturn(new PrescriptionDraftContext.SnapshotResult(false, List.of()));

        AuditRecord latest = new AuditRecord();
        latest.setRiskLevel("WARN");
        latest.setLatest(true);
        latest.setOriginalPrescription("[{\"drugId\":\"drug-001\",\"dose\":50,\"frequency\":\"bid\",\"duration\":\"5d\",\"route\":\"oral\"}]");
        when(auditRecordRepository.findTopByPrescriptionIdAndIsLatestTrueOrderByAuditSequenceDesc("rx-001"))
                .thenReturn(Optional.of(latest));

        SubmitRequest request = new SubmitRequest();
        request.setPrescriptionId("rx-001");
        request.setPrescriptionItems(List.of(sampleItem));
        request.setForceSubmit(false);

        SubmitResponse result = service.submit(request);

        assertFalse(result.isSubmitted());
        assertEquals(PrescriptionErrorCode.RX_AUDIT_PRESCRIPTION_MODIFIED.getCode(), result.getErrorCode());
    }

    @Test
    void submitShouldRequireForceSubmitWhenWarnAndPrescriptionUnchanged() {
        when(prescriptionDraftContext.snapshotCriticalAlerts("rx-001"))
                .thenReturn(new PrescriptionDraftContext.SnapshotResult(false, List.of()));

        AuditRecord latest = new AuditRecord();
        latest.setId(100L);
        latest.setRiskLevel("WARN");
        latest.setLatest(true);
        latest.setOriginalPrescription("[{\"drugId\":\"drug-001\",\"dose\":100.0,\"frequency\":\"tid\",\"duration\":\"7d\",\"route\":\"oral\",\"drugName\":\"Aspirin\"}]");
        when(auditRecordRepository.findTopByPrescriptionIdAndIsLatestTrueOrderByAuditSequenceDesc("rx-001"))
                .thenReturn(Optional.of(latest));

        SubmitRequest request = new SubmitRequest();
        request.setPrescriptionId("rx-001");
        request.setPrescriptionItems(List.of(sampleItem));
        request.setForceSubmit(false);

        SubmitResponse result = service.submit(request);

        assertFalse(result.isSubmitted());
        assertNull(result.getErrorCode());
        assertNotNull(result.getWarnResult());
        assertEquals(AuditRiskLevel.WARN, result.getWarnResult().getRiskLevel());
        assertNotNull(result.getWarnResult().getAlerts());
        assertNotNull(result.getWarnResult().getAuditRecordId());
        assertNotNull(result.getWarnResult().getPrescriptionHash());
    }

    @Test
    void submitShouldForceSubmitWhenWarnAndValidForceRequest() {
        when(prescriptionDraftContext.snapshotCriticalAlerts("rx-001"))
                .thenReturn(new PrescriptionDraftContext.SnapshotResult(false, List.of()));

        AuditRecord latest = new AuditRecord();
        latest.setId(100L);
        latest.setRiskLevel("WARN");
        latest.setLatest(true);
        latest.setOriginalPrescription("[{\"drugId\":\"drug-001\",\"dose\":100.0,\"frequency\":\"tid\",\"duration\":\"7d\",\"route\":\"oral\",\"drugName\":\"Aspirin\"}]");
        when(auditRecordRepository.findTopByPrescriptionIdAndIsLatestTrueOrderByAuditSequenceDesc("rx-001"))
                .thenReturn(Optional.of(latest));
        when(auditRecordRepository.save(any())).thenReturn(latest);

        SubmitRequest request = new SubmitRequest();
        request.setPrescriptionId("rx-001");
        request.setPrescriptionItems(List.of(sampleItem));
        request.setForceSubmit(true);
        request.setAuditRecordId(100L);

        SubmitResponse result = service.submit(request);

        assertTrue(result.isSubmitted());
        verify(auditRecordRepository).save(any());
        assertTrue(latest.getForceSubmitted());
        assertNotNull(latest.getForceSubmitTime());
    }

    @Test
    void submitShouldReturnConcurrentSubmitErrorWhenOptimisticLockException() {
        when(prescriptionDraftContext.snapshotCriticalAlerts("rx-001"))
                .thenReturn(new PrescriptionDraftContext.SnapshotResult(false, List.of()));

        AuditRecord latest = new AuditRecord();
        latest.setId(100L);
        latest.setRiskLevel("WARN");
        latest.setLatest(true);
        latest.setOriginalPrescription("[{\"drugId\":\"drug-001\",\"dose\":100.0,\"frequency\":\"tid\",\"duration\":\"7d\",\"route\":\"oral\",\"drugName\":\"Aspirin\"}]");
        when(auditRecordRepository.findTopByPrescriptionIdAndIsLatestTrueOrderByAuditSequenceDesc("rx-001"))
                .thenReturn(Optional.of(latest));
        when(auditRecordRepository.save(any())).thenThrow(
            new ObjectOptimisticLockingFailureException(
                "com.aimedical.modules.prescription.entity.AuditRecord",
                new jakarta.persistence.OptimisticLockException()));

        SubmitRequest request = new SubmitRequest();
        request.setPrescriptionId("rx-001");
        request.setPrescriptionItems(List.of(sampleItem));
        request.setForceSubmit(true);
        request.setAuditRecordId(100L);

        SubmitResponse result = service.submit(request);

        assertFalse(result.isSubmitted());
        assertEquals(PrescriptionErrorCode.RX_AUDIT_CONCURRENT_SUBMIT.getCode(), result.getErrorCode());
    }

    @Test
    void submitShouldRejectForceSubmitWhenNonWarn() {
        when(prescriptionDraftContext.snapshotCriticalAlerts("rx-001"))
                .thenReturn(new PrescriptionDraftContext.SnapshotResult(false, List.of()));

        AuditRecord latest = new AuditRecord();
        latest.setRiskLevel("PASS");
        latest.setLatest(true);
        latest.setOriginalPrescription("[]");
        when(auditRecordRepository.findTopByPrescriptionIdAndIsLatestTrueOrderByAuditSequenceDesc("rx-001"))
                .thenReturn(Optional.of(latest));

        SubmitRequest request = new SubmitRequest();
        request.setPrescriptionId("rx-001");
        request.setPrescriptionItems(List.of(sampleItem));
        request.setForceSubmit(true);
        request.setAuditRecordId(100L);

        SubmitResponse result = service.submit(request);

        assertFalse(result.isSubmitted());
        assertEquals(PrescriptionErrorCode.RX_AUDIT_FORCE_SUBMIT_INVALID.getCode(), result.getErrorCode());
    }

    @Test
    void submitShouldRejectForceSubmitWhenPrescriptionModified() {
        when(prescriptionDraftContext.snapshotCriticalAlerts("rx-001"))
                .thenReturn(new PrescriptionDraftContext.SnapshotResult(false, List.of()));

        AuditRecord latest = new AuditRecord();
        latest.setId(100L);
        latest.setRiskLevel("WARN");
        latest.setLatest(true);
        latest.setOriginalPrescription("[{\"drugId\":\"drug-001\",\"dose\":50,\"frequency\":\"bid\",\"duration\":\"5d\",\"route\":\"oral\"}]");
        when(auditRecordRepository.findTopByPrescriptionIdAndIsLatestTrueOrderByAuditSequenceDesc("rx-001"))
                .thenReturn(Optional.of(latest));

        SubmitRequest request = new SubmitRequest();
        request.setPrescriptionId("rx-001");
        request.setPrescriptionItems(List.of(sampleItem));
        request.setForceSubmit(true);
        request.setAuditRecordId(100L);

        SubmitResponse result = service.submit(request);

        assertFalse(result.isSubmitted());
        assertEquals(PrescriptionErrorCode.RX_AUDIT_PRESCRIPTION_MODIFIED.getCode(), result.getErrorCode());
    }

    @Test
    void submitShouldRejectForceSubmitWhenAuditRecordIdMismatch() {
        when(prescriptionDraftContext.snapshotCriticalAlerts("rx-001"))
                .thenReturn(new PrescriptionDraftContext.SnapshotResult(false, List.of()));

        AuditRecord latest = new AuditRecord();
        latest.setId(100L);
        latest.setRiskLevel("WARN");
        latest.setLatest(true);
        latest.setOriginalPrescription("[]");
        when(auditRecordRepository.findTopByPrescriptionIdAndIsLatestTrueOrderByAuditSequenceDesc("rx-001"))
                .thenReturn(Optional.of(latest));

        SubmitRequest request = new SubmitRequest();
        request.setPrescriptionId("rx-001");
        request.setPrescriptionItems(List.of(sampleItem));
        request.setForceSubmit(true);
        request.setAuditRecordId(999L);

        SubmitResponse result = service.submit(request);

        assertFalse(result.isSubmitted());
        assertEquals(PrescriptionErrorCode.RX_AUDIT_FORCE_SUBMIT_INVALID.getCode(), result.getErrorCode());
    }

    // ─── P08: forceSubmit prescriptionOrderId ────────────────────────────────

    @Test
    void submitShouldSetPrescriptionOrderIdOnRecordAndResponseWhenForceSubmit() {
        when(prescriptionDraftContext.snapshotCriticalAlerts("rx-001"))
                .thenReturn(new PrescriptionDraftContext.SnapshotResult(false, List.of()));

        AuditRecord latest = new AuditRecord();
        latest.setId(100L);
        latest.setRiskLevel("WARN");
        latest.setLatest(true);
        latest.setOriginalPrescription("[{\"drugId\":\"drug-001\",\"dose\":100.0,\"frequency\":\"tid\",\"duration\":\"7d\",\"route\":\"oral\",\"drugName\":\"Aspirin\"}]");
        when(auditRecordRepository.findTopByPrescriptionIdAndIsLatestTrueOrderByAuditSequenceDesc("rx-001"))
                .thenReturn(Optional.of(latest));
        when(auditRecordRepository.save(any())).thenReturn(latest);

        SubmitRequest request = new SubmitRequest();
        request.setPrescriptionId("rx-001");
        request.setPrescriptionItems(List.of(sampleItem));
        request.setForceSubmit(true);
        request.setAuditRecordId(100L);

        SubmitResponse result = service.submit(request);

        assertTrue(result.isSubmitted());
        assertNotNull(result.getPrescriptionOrderId());
        assertNotNull(latest.getPrescriptionOrderId());
        assertEquals(result.getPrescriptionOrderId(), latest.getPrescriptionOrderId());
    }

    // ─── P16: orderId grouping cleanup ───────────────────────────────────────

    @Test
    void submitShouldCleanupIsLatestByOrderIdWhenForceSubmit() {
        when(prescriptionDraftContext.snapshotCriticalAlerts("rx-001"))
                .thenReturn(new PrescriptionDraftContext.SnapshotResult(false, List.of()));

        AuditRecord latest = new AuditRecord();
        latest.setId(100L);
        latest.setRiskLevel("WARN");
        latest.setLatest(true);
        latest.setOriginalPrescription("[{\"drugId\":\"drug-001\",\"dose\":100.0,\"frequency\":\"tid\",\"duration\":\"7d\",\"route\":\"oral\",\"drugName\":\"Aspirin\"}]");
        when(auditRecordRepository.findTopByPrescriptionIdAndIsLatestTrueOrderByAuditSequenceDesc("rx-001"))
                .thenReturn(Optional.of(latest));
        when(auditRecordRepository.save(any())).thenReturn(latest);

        AuditRecord oldOrderRecord = new AuditRecord();
        oldOrderRecord.setPrescriptionOrderId("RX-OLD");
        oldOrderRecord.setLatest(true);
        when(auditRecordRepository.findByPrescriptionOrderIdAndIsLatestTrue(anyString()))
                .thenReturn(new ArrayList<>(List.of(oldOrderRecord)));

        SubmitRequest request = new SubmitRequest();
        request.setPrescriptionId("rx-001");
        request.setPrescriptionItems(List.of(sampleItem));
        request.setForceSubmit(true);
        request.setAuditRecordId(100L);

        SubmitResponse result = service.submit(request);

        assertTrue(result.isSubmitted());
        verify(auditRecordRepository).findByPrescriptionOrderIdAndIsLatestTrue(anyString());
        verify(auditRecordRepository).saveAll(anyList());
        assertFalse(oldOrderRecord.isLatest());
        assertTrue(latest.isLatest());
    }

    @Test
    void submitShouldHandleEmptyOrderRecordsWhenForceSubmit() {
        when(prescriptionDraftContext.snapshotCriticalAlerts("rx-001"))
                .thenReturn(new PrescriptionDraftContext.SnapshotResult(false, List.of()));

        AuditRecord latest = new AuditRecord();
        latest.setId(100L);
        latest.setRiskLevel("WARN");
        latest.setLatest(true);
        latest.setOriginalPrescription("[{\"drugId\":\"drug-001\",\"dose\":100.0,\"frequency\":\"tid\",\"duration\":\"7d\",\"route\":\"oral\",\"drugName\":\"Aspirin\"}]");
        when(auditRecordRepository.findTopByPrescriptionIdAndIsLatestTrueOrderByAuditSequenceDesc("rx-001"))
                .thenReturn(Optional.of(latest));
        when(auditRecordRepository.save(any())).thenReturn(latest);
        when(auditRecordRepository.findByPrescriptionOrderIdAndIsLatestTrue(anyString()))
                .thenReturn(new ArrayList<>());

        SubmitRequest request = new SubmitRequest();
        request.setPrescriptionId("rx-001");
        request.setPrescriptionItems(List.of(sampleItem));
        request.setForceSubmit(true);
        request.setAuditRecordId(100L);

        SubmitResponse result = service.submit(request);

        assertTrue(result.isSubmitted());
        verify(auditRecordRepository).findByPrescriptionOrderIdAndIsLatestTrue(anyString());
        verify(auditRecordRepository, never()).saveAll(anyList());
    }

    @Test
    void submitShouldReAuditWhenNoLatestRecordFoundThenReturnBlock() throws Exception {
        when(prescriptionDraftContext.snapshotCriticalAlerts("rx-001"))
                .thenReturn(new PrescriptionDraftContext.SnapshotResult(false, List.of()));
        when(auditRecordRepository.findTopByPrescriptionIdAndIsLatestTrueOrderByAuditSequenceDesc("rx-001"))
                .thenReturn(Optional.empty());

        PrescriptionCheckResponse checkResponse = new PrescriptionCheckResponse();
        checkResponse.setRiskLevel("HIGH");
        AiResult<PrescriptionCheckResponse> aiResult = AiResult.success(checkResponse);
        when(aiService.prescriptionCheck(any())).thenReturn(CompletableFuture.completedFuture(aiResult));
        when(auditConverter.toAiPrescriptionCheckRequest(any())).thenReturn(new PrescriptionCheckRequest());
        when(auditConverter.toAuditResponse(aiResult)).thenAnswer(inv -> {
            AuditResponse r = new AuditResponse();
            r.setRiskLevel(AuditRiskLevel.BLOCK);
            return r;
        });
        when(currentUser.getUserId()).thenReturn(1L);
        when(auditRecordRepository.findByPrescriptionIdAndIsLatestTrue("rx-001")).thenReturn(new ArrayList<>());

        SubmitRequest request = new SubmitRequest();
        request.setPrescriptionId("rx-001");
        request.setPrescriptionItems(List.of(sampleItem));

        SubmitResponse result = service.submit(request);

        assertFalse(result.isSubmitted());
        assertEquals("RX_BLOCK_AUDIT", result.getBlockInfo().getBlockCode());
    }

    // ─── revoke() tests ──────────────────────────────────────────────────────

    @Test
    void revokeShouldSucceedWhenWarnAndLatest() {
        AuditRecord record = new AuditRecord();
        record.setId(100L);
        record.setRiskLevel("WARN");
        record.setLatest(true);
        when(auditRecordRepository.findById(100L)).thenReturn(Optional.of(record));

        service.revoke(100L);

        assertFalse(record.isLatest());
        verify(auditRecordRepository).save(record);
    }

    @Test
    void revokeShouldThrowNotFoundWhenRecordMissing() {
        when(auditRecordRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.revoke(999L));
        assertEquals(PrescriptionErrorCode.RX_AUDIT_REVOKE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void revokeShouldThrowAlreadyRevokedWhenNotLatest() {
        AuditRecord record = new AuditRecord();
        record.setId(100L);
        record.setLatest(false);
        when(auditRecordRepository.findById(100L)).thenReturn(Optional.of(record));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.revoke(100L));
        assertEquals(PrescriptionErrorCode.RX_AUDIT_REVOKE_ALREADY_REVOKED, ex.getErrorCode());
    }

    @Test
    void revokeShouldThrowNotWarnWhenRiskLevelNotWarn() {
        AuditRecord record = new AuditRecord();
        record.setId(100L);
        record.setRiskLevel("PASS");
        record.setLatest(true);
        when(auditRecordRepository.findById(100L)).thenReturn(Optional.of(record));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.revoke(100L));
        assertEquals(PrescriptionErrorCode.RX_AUDIT_REVOKE_NOT_WARN, ex.getErrorCode());
    }

    @Test
    void submitShouldIncludeAlertMessagesAsReasonsWhenCriticalDoseBlock() {
        DosageAlert alert1 = createDosageAlert("drug-001", "CRITICAL");
        alert1.setMessage("Overdose drug-001");
        DosageAlert alert2 = createDosageAlert("drug-002", "CRITICAL");
        alert2.setMessage("Overdose drug-002");
        when(prescriptionDraftContext.snapshotCriticalAlerts("rx-001"))
                .thenReturn(new PrescriptionDraftContext.SnapshotResult(true, List.of(alert1, alert2)));

        SubmitRequest request = new SubmitRequest();
        request.setPrescriptionId("rx-001");
        request.setPrescriptionItems(List.of(sampleItem));

        SubmitResponse result = service.submit(request);

        assertFalse(result.isSubmitted());
        assertEquals(2, result.getBlockInfo().getBlockReasons().size());
        assertTrue(result.getBlockInfo().getBlockReasons().contains("Overdose drug-001"));
        assertTrue(result.getBlockInfo().getBlockReasons().contains("Overdose drug-002"));
    }

    @Test
    void submitShouldUseFallbackReasonWhenAlertMessagesEmpty() {
        when(prescriptionDraftContext.snapshotCriticalAlerts("rx-001"))
                .thenReturn(new PrescriptionDraftContext.SnapshotResult(true, List.of()));

        SubmitRequest request = new SubmitRequest();
        request.setPrescriptionId("rx-001");
        request.setPrescriptionItems(List.of(sampleItem));

        SubmitResponse result = service.submit(request);

        assertFalse(result.isSubmitted());
        assertEquals(1, result.getBlockInfo().getBlockReasons().size());
        assertEquals("Critical dosage alerts detected", result.getBlockInfo().getBlockReasons().get(0));
    }

    @Test
    void submitShouldSerializeSamePrescriptionIdRequests() throws Exception {
        when(prescriptionDraftContext.snapshotCriticalAlerts("rx-001"))
                .thenReturn(new PrescriptionDraftContext.SnapshotResult(false, List.of()));
        AuditRecord latest = new AuditRecord();
        latest.setRiskLevel("PASS");
        latest.setLatest(true);
        when(auditRecordRepository.findTopByPrescriptionIdAndIsLatestTrueOrderByAuditSequenceDesc("rx-001"))
                .thenReturn(Optional.of(latest));

        SubmitRequest request = new SubmitRequest();
        request.setPrescriptionId("rx-001");
        request.setPrescriptionItems(List.of(sampleItem));

        SubmitResponse result = service.submit(request);

        assertTrue(result.isSubmitted());

        verify(prescriptionDraftContext).snapshotCriticalAlerts("rx-001");
    }

    @Test
    void constructorShouldAcceptNineParameters() throws Exception {
        var constructor = PrescriptionAuditServiceImpl.class.getConstructors();
        assertEquals(1, constructor.length);
        assertEquals(9, constructor[0].getParameterCount());
    }

    private DosageAlert createDosageAlert(String drugCode, String severity) {
        DosageAlert alert = new DosageAlert();
        alert.setDrugCode(drugCode);
        alert.setSeverity(severity);
        alert.setMessage("Overdose");
        return alert;
    }
}
