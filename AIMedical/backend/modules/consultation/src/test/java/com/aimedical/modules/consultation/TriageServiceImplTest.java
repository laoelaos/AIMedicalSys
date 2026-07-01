package com.aimedical.modules.consultation;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.modules.ai.api.AiResult;
import com.aimedical.modules.ai.api.AiService;
import com.aimedical.modules.ai.api.dto.triage.RecommendedDepartment;
import com.aimedical.modules.ai.api.dto.triage.TriageResponse;
import com.aimedical.modules.commonmodule.doctor.AvailableDoctor;
import com.aimedical.modules.commonmodule.doctor.DoctorFacade;
import com.aimedical.modules.commonmodule.store.SessionStore;
import com.aimedical.modules.consultation.converter.TriageConverter;
import com.aimedical.modules.consultation.dialogue.DialogueSession;
import com.aimedical.modules.consultation.dialogue.DialogueSessionManager;
import com.aimedical.modules.consultation.dto.DialogueCreateRequest;
import com.aimedical.modules.consultation.entity.TriageRecord;
import com.aimedical.modules.consultation.exception.TriageErrorCode;
import com.aimedical.modules.consultation.fallback.DepartmentFallbackProvider;
import com.aimedical.modules.consultation.repository.TriageRecordRepository;
import com.aimedical.modules.consultation.rule.MatchResult;
import com.aimedical.modules.consultation.rule.TriageRuleEngine;
import com.aimedical.modules.consultation.service.impl.TriageServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import static org.junit.jupiter.api.Assertions.*;

class TriageServiceImplTest {

    private static final String VALID_UUID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String VALID_UUID_2 = "f47ac10b-58cc-4372-a567-0e02b2c3d479";

    private StubAiService aiService;
    private StubTriageRuleEngine ruleEngine;
    private StubFallbackProvider fallbackProvider;
    private StubDoctorFacade doctorFacade;
    private StubSessionStore sessionStore;
    private StubTriageRecordRepository recordRepository;
    private StubTransactionManager transactionManager;
    private TriageConverter converter;
    private ObjectMapper objectMapper;
    private DialogueSessionManager sessionManager;
    private TriageServiceImpl service;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        aiService = new StubAiService();
        ruleEngine = new StubTriageRuleEngine();
        fallbackProvider = new StubFallbackProvider();
        doctorFacade = new StubDoctorFacade();
        sessionStore = new StubSessionStore();
        recordRepository = new StubTriageRecordRepository();
        sessionManager = new DialogueSessionManager(sessionStore, recordRepository);
        converter = new TriageConverter();
        objectMapper = new ObjectMapper();
        transactionManager = new StubTransactionManager();
        service = new TriageServiceImpl(aiService, ruleEngine, fallbackProvider, doctorFacade,
                sessionManager, recordRepository, converter, objectMapper, transactionManager, 5L, 10L);

        Logger triageLogger = (Logger) org.slf4j.LoggerFactory.getLogger(TriageServiceImpl.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        triageLogger.addAppender(logAppender);
    }

    @Test
    void shouldPerformTriageWithAiSuccess() {
        DialogueCreateRequest request = createBasicRequest();
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiTriageResponse(VALID_UUID)));

        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

        assertNotNull(result);
        assertEquals(VALID_UUID, result.getSessionId());
        assertFalse(result.isDegraded());
    }

    @Test
    void shouldFallbackToRuleEngineWhenAiFails() {
        DialogueCreateRequest request = createBasicRequest();
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.failure("AI_ERROR"));

        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

        assertNotNull(result);
        assertTrue(result.isDegraded());
        assertEquals(1, result.getDepartments().size());
        assertEquals("dept-01", result.getDepartments().get(0).getDepartmentId());
    }

    @Test
    void shouldFallbackToDefaultDepartmentsWhenRuleEngineReturnsEmpty() {
        DialogueCreateRequest request = createBasicRequest();
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.failure("AI_ERROR"));
        ruleEngine.returnEmpty = true;

        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

        assertNotNull(result);
        assertTrue(result.isDegraded());
        assertEquals(1, result.getDepartments().size());
        assertEquals("fallback-dept-id", result.getDepartments().get(0).getDepartmentId());
    }

    @Test
    void shouldSetFallbackHintAfterThreeAiFailures() {
        DialogueCreateRequest request = createBasicRequest();
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.failure("AI_ERROR"));

        service.triage(request);
        service.triage(request);
        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

        assertTrue(result.isDegraded());
        assertEquals("AI 服务持续不可用，建议稍后重试", result.getFallbackHint());
    }

    @Test
    void shouldNotSetFallbackHintAfterTwoAiFailures() {
        DialogueCreateRequest request = createBasicRequest();
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.failure("AI_ERROR"));

        service.triage(request);
        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

        assertTrue(result.isDegraded());
        assertNull(result.getFallbackHint());
    }

    @Test
    void shouldIncrementFailCountOnExecutionException() {
        DialogueCreateRequest request = createBasicRequest();
        CompletableFuture<AiResult<TriageResponse>> exFuture = new CompletableFuture<>();
        exFuture.completeExceptionally(new ExecutionException(new RuntimeException("AI error")));
        aiService.resultFuture = exFuture;

        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

        assertTrue(result.isDegraded());
        assertNull(result.getFallbackHint());
    }

    @Test
    void shouldRequireThreeExecutionExceptionsForFallbackHint() {
        DialogueCreateRequest request = createBasicRequest();
        CompletableFuture<AiResult<TriageResponse>> exFuture = new CompletableFuture<>();
        exFuture.completeExceptionally(new ExecutionException(new RuntimeException("AI error")));
        aiService.resultFuture = exFuture;

        service.triage(request);
        service.triage(request);
        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

        assertTrue(result.isDegraded());
        assertEquals("AI 服务持续不可用，建议稍后重试", result.getFallbackHint());
    }

    @Test
    void shouldIncrementFailCountOnInterruptedException() {
        DialogueCreateRequest request = createBasicRequest();
        CompletableFuture<AiResult<TriageResponse>> intFuture = new CompletableFuture<>() {
            @Override
            public AiResult<TriageResponse> get() throws InterruptedException {
                throw new InterruptedException("interrupted");
            }
        };
        aiService.resultFuture = intFuture;

        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

        assertTrue(result.isDegraded());
        assertNull(result.getFallbackHint());
    }

    @Test
    void shouldNotDoubleCountWhenMixedFailurePaths() {
        DialogueCreateRequest request = createBasicRequest();
        CompletableFuture<AiResult<TriageResponse>> exFuture = new CompletableFuture<>();
        exFuture.completeExceptionally(new ExecutionException(new RuntimeException("AI error")));
        aiService.resultFuture = exFuture;
        service.triage(request);

        aiService.resultFuture = CompletableFuture.completedFuture(AiResult.failure("AI_ERROR"));
        service.triage(request);
        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

        assertTrue(result.isDegraded());
        assertEquals("AI 服务持续不可用，建议稍后重试", result.getFallbackHint());
    }

    @Test
    void shouldFallbackOnTimeout() {
        DialogueCreateRequest request = createBasicRequest();
        aiService.resultFuture = new CompletableFuture<>();
        TriageServiceImpl timeoutService = new TriageServiceImpl(aiService, ruleEngine,
                fallbackProvider, doctorFacade, sessionManager, recordRepository,
                converter, objectMapper, transactionManager, 0L, 10L);

        com.aimedical.modules.consultation.dto.TriageResponse result = timeoutService.triage(request);

        assertTrue(result.isDegraded());
    }

    @Test
    void shouldResetAiFailCountOnSuccessfulTriage() {
        DialogueCreateRequest request = createBasicRequest();
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.failure("AI_ERROR"));
        service.triage(request);

        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiTriageResponse(VALID_UUID)));
        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

        assertFalse(result.isDegraded());
    }

    @Test
    void shouldPersistTriageRecordOnTriage() {
        DialogueCreateRequest request = createBasicRequest();
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiTriageResponse(VALID_UUID)));

        service.triage(request);

        assertTrue(recordRepository.saved);
    }

    @Test
    void shouldUpdateExistingTriageRecordOnSecondCallWithSameSessionId() {
        DialogueCreateRequest request = createBasicRequest();
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiTriageResponse(VALID_UUID)));

        service.triage(request);
        assertTrue(recordRepository.saved);

        request.setChiefComplaint("头痛五天");
        service.triage(request);

        assertNotNull(recordRepository.record);
        assertEquals("头痛五天", recordRepository.record.getChiefComplaint());
    }

    @Test
    void shouldSetCorrectedChiefComplaintFromRequestToSession() {
        DialogueCreateRequest request = createBasicRequest();
        request.setCorrectedChiefComplaint("前端修正：偏头痛");
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiTriageResponse(VALID_UUID)));

        service.triage(request);

        assertTrue(recordRepository.saved);
        assertNotNull(recordRepository.record);
        assertEquals("前端修正：偏头痛", recordRepository.record.getCorrectedChiefComplaint());
    }

    @Test
    void shouldWriteBackCorrectedChiefComplaintFromAiResultToSessionAndRecord() {
        DialogueCreateRequest request = createBasicRequest();
        TriageResponse aiData = createAiTriageResponse(VALID_UUID);
        aiData.setCorrectedChiefComplaint("AI修正：偏头痛");
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(aiData));

        service.triage(request);

        assertTrue(recordRepository.saved);
        assertNotNull(recordRepository.record);
        assertEquals("AI修正：偏头痛", recordRepository.record.getCorrectedChiefComplaint());
    }

    @Test
    void shouldOverrideCorrectedChiefComplaintFromAiResultOverRequest() {
        DialogueCreateRequest request = createBasicRequest();
        request.setCorrectedChiefComplaint("前端修正：偏头痛");
        TriageResponse aiData = createAiTriageResponse(VALID_UUID);
        aiData.setCorrectedChiefComplaint("AI修正：紧张型头痛");
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(aiData));

        service.triage(request);

        assertTrue(recordRepository.saved);
        assertNotNull(recordRepository.record);
        assertEquals("AI修正：紧张型头痛", recordRepository.record.getCorrectedChiefComplaint());
    }

    @Test
    void shouldNotSetCorrectedChiefComplaintOnRecordWhenSessionCcIsNull() {
        DialogueCreateRequest request = createBasicRequest();
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiTriageResponse(VALID_UUID)));

        service.triage(request);

        assertTrue(recordRepository.saved);
        assertNotNull(recordRepository.record);
        assertNull(recordRepository.record.getCorrectedChiefComplaint());
    }

    @Test
    void shouldSelectDepartmentWithOverwriteTrue() {
        TriageRecord record = new TriageRecord();
        record.setSessionId(VALID_UUID);
        record.setFinalDepartmentId(null);
        recordRepository.record = record;

        com.aimedical.modules.consultation.dto.TriageResponse result = service.selectDepartment(
                VALID_UUID, "dept-01", "内科");

        assertNotNull(result);
        assertEquals("Department selected", result.getReason());
        assertTrue(recordRepository.saved);
    }

    @Test
    void shouldSelectDepartmentWhenFinalIsNull() {
        TriageRecord record = new TriageRecord();
        record.setSessionId(VALID_UUID);
        record.setFinalDepartmentId(null);
        recordRepository.record = record;

        com.aimedical.modules.consultation.dto.TriageResponse result = service.selectDepartment(
                VALID_UUID, "dept-01", "内科");

        assertNotNull(result);
        assertTrue(recordRepository.saved);
    }

    @Test
    void shouldThrowBusinessExceptionWhenRecordNotFound() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.selectDepartment("non-existent", "dept-01", "内科"));
        assertEquals(TriageErrorCode.TRIAGE_SESSION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void shouldOverwriteExistingFinalDepartment() {
        TriageRecord record = new TriageRecord();
        record.setSessionId(VALID_UUID);
        record.setFinalDepartmentId("existing-dept");
        record.setFinalDepartmentName("existing-name");
        recordRepository.record = record;

        com.aimedical.modules.consultation.dto.TriageResponse result = service.selectDepartment(
                VALID_UUID, "dept-01", "内科");

        assertNotNull(result);
        assertTrue(recordRepository.saved);
        assertEquals("dept-01", recordRepository.record.getFinalDepartmentId());
        assertEquals("内科", recordRepository.record.getFinalDepartmentName());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenSelectDepartmentSessionIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                service.selectDepartment(null, "dept-01", "内科"));
    }

    @Test
    void shouldInsertNewTriageRecordWhenNoExistingRecord() {
        DialogueCreateRequest request = createBasicRequest();
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiTriageResponse(VALID_UUID)));

        service.triage(request);

        assertTrue(recordRepository.saved);
        assertNotNull(recordRepository.record);
        assertEquals(VALID_UUID, recordRepository.record.getSessionId());
        assertEquals("P001", recordRepository.record.getPatientId());
        assertEquals("头痛三天", recordRepository.record.getChiefComplaint());
    }

    @Test
    void shouldUpdateExistingTriageRecordWhenRecordAlreadyExists() {
        DialogueCreateRequest firstRequest = createBasicRequest();
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiTriageResponse(VALID_UUID)));
        service.triage(firstRequest);
        assertTrue(recordRepository.saved);

        DialogueCreateRequest secondRequest = createBasicRequest();
        secondRequest.setChiefComplaint("头痛五天");
        secondRequest.setPatientId("P001");
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiTriageResponse(VALID_UUID)));
        service.triage(secondRequest);

        assertEquals("头痛五天", recordRepository.record.getChiefComplaint());
        assertEquals("P001", recordRepository.record.getPatientId());
    }

    @Test
    void shouldUseTransactionTemplateForSaveTriageRecord() {
        DialogueCreateRequest request = createBasicRequest();
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiTriageResponse(VALID_UUID)));

        service.triage(request);

        assertTrue(transactionManager.getTransactionCalled);
        assertTrue(transactionManager.commitCalled);
    }

    @Test
    void shouldSaveRuleMatchedDepartmentsWhenDegraded() {
        DialogueCreateRequest request = createBasicRequest();
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.failure("AI_ERROR"));

        service.triage(request);

        assertTrue(recordRepository.saved);
        assertNotNull(recordRepository.record);
        assertTrue(recordRepository.record.getDegraded());
        assertNotNull(recordRepository.record.getRuleMatchedDepartments());
        assertNull(recordRepository.record.getAiRecommendedDepartments());
    }

    @Test
    void shouldSaveAiRecommendedDepartmentsWhenNotDegraded() {
        DialogueCreateRequest request = createBasicRequest();
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiTriageResponse(VALID_UUID)));

        service.triage(request);

        assertTrue(recordRepository.saved);
        assertNotNull(recordRepository.record);
        assertNull(recordRepository.record.getRuleMatchedDepartments());
        assertNotNull(recordRepository.record.getAiRecommendedDepartments());
        assertFalse(recordRepository.record.getDegraded());
    }

    @Test
    void shouldNotSetDepartmentFieldsWhenFinalDepartmentsJsonIsNull() {
        DialogueCreateRequest request = createBasicRequest();
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.failure("AI_ERROR"));
        ruleEngine.returnEmpty = true;
        fallbackProvider.returnEmpty = true;

        service.triage(request);

        assertTrue(recordRepository.saved);
        assertNotNull(recordRepository.record);
        assertTrue(recordRepository.record.getDegraded());
        assertNull(recordRepository.record.getRuleMatchedDepartments());
        assertNull(recordRepository.record.getAiRecommendedDepartments());
    }

    @Test
    void shouldLogErrorWhenJsonSerializationFailsInSaveTriageRecord() {
        ObjectMapper failingMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("Simulated JSON error") {};
            }
        };
        TriageServiceImpl testService = new TriageServiceImpl(aiService, ruleEngine,
                fallbackProvider, doctorFacade, sessionManager, recordRepository,
                converter, failingMapper, transactionManager, 5L, 10L);

        DialogueCreateRequest request = createBasicRequest();
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.failure("AI_ERROR"));

        testService.triage(request);

        assertEquals(1, logAppender.list.size());
        ILoggingEvent event = logAppender.list.get(0);
        assertEquals(Level.ERROR, event.getLevel());
        assertTrue(event.getFormattedMessage().contains("Failed to serialize triage record JSON fields"));
    }

    @Test
    void shouldRollbackTransactionWhenExceptionOccursInSave() {
        DialogueCreateRequest request = createBasicRequest();
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiTriageResponse(VALID_UUID)));
        recordRepository.throwOnSave = true;

        assertThrows(RuntimeException.class, () -> service.triage(request));
        assertTrue(transactionManager.rollbackCalled);
    }

    @Test
    void shouldMapDoctorsFromSingleDepartment() {
        DialogueCreateRequest request = createBasicRequest();
        TriageResponse aiData = createAiTriageResponse(VALID_UUID, "dept-01");
        doctorFacade.addDoctors("dept-01", List.of(
                new AvailableDoctor("doc-1", "Dr. Wang", "dept-01", 10)));
        aiService.resultFuture = CompletableFuture.completedFuture(AiResult.success(aiData));

        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

        assertEquals(1, result.getDoctors().size());
        com.aimedical.modules.consultation.dto.RecommendedDoctor doc = result.getDoctors().get(0);
        assertEquals("doc-1", doc.getDoctorId());
        assertEquals("Dr. Wang", doc.getDoctorName());
        assertEquals("dept-01", doc.getDepartmentId());
        assertEquals(10, doc.getAvailableSlotCount());
        assertEquals(0f, doc.getScore(), 0.001f);
    }

    @Test
    void shouldReturnEmptyWhenDepartmentsIsNull() {
        DialogueCreateRequest request = createBasicRequest();
        TriageResponse aiData = createAiTriageResponse(VALID_UUID, "dept-01");
        aiData.setRecommendedDepartments(null);
        aiService.resultFuture = CompletableFuture.completedFuture(AiResult.success(aiData));

        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

        assertNotNull(result.getDoctors());
        assertTrue(result.getDoctors().isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenDepartmentsIsEmpty() {
        DialogueCreateRequest request = createBasicRequest();
        TriageResponse aiData = createAiTriageResponse(VALID_UUID, "dept-01");
        aiData.setRecommendedDepartments(Collections.emptyList());
        aiService.resultFuture = CompletableFuture.completedFuture(AiResult.success(aiData));

        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

        assertNotNull(result.getDoctors());
        assertTrue(result.getDoctors().isEmpty());
    }

    @Test
    void shouldLimitToFiveDoctorsAcrossDepartments() {
        DialogueCreateRequest request = createBasicRequest();
        TriageResponse aiData = createAiTriageResponse(VALID_UUID, "dept-01", "dept-02");
        doctorFacade.addDoctors("dept-01", List.of(
                new AvailableDoctor("doc-1", "Dr. A", "dept-01", 5),
                new AvailableDoctor("doc-2", "Dr. B", "dept-01", 4),
                new AvailableDoctor("doc-3", "Dr. C", "dept-01", 3)));
        doctorFacade.addDoctors("dept-02", List.of(
                new AvailableDoctor("doc-4", "Dr. D", "dept-02", 2),
                new AvailableDoctor("doc-5", "Dr. E", "dept-02", 1),
                new AvailableDoctor("doc-6", "Dr. F", "dept-02", 0)));
        aiService.resultFuture = CompletableFuture.completedFuture(AiResult.success(aiData));

        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

        assertEquals(5, result.getDoctors().size());
        assertEquals(5, result.getDoctors().get(0).getAvailableSlotCount());
        assertEquals(4, result.getDoctors().get(1).getAvailableSlotCount());
        assertEquals(3, result.getDoctors().get(2).getAvailableSlotCount());
        assertEquals(2, result.getDoctors().get(3).getAvailableSlotCount());
        assertEquals(1, result.getDoctors().get(4).getAvailableSlotCount());
    }

    @Test
    void shouldSortDoctorsBySlotCountDescending() {
        DialogueCreateRequest request = createBasicRequest();
        TriageResponse aiData = createAiTriageResponse(VALID_UUID, "dept-01");
        doctorFacade.addDoctors("dept-01", List.of(
                new AvailableDoctor("doc-1", "Dr. A", "dept-01", 1),
                new AvailableDoctor("doc-2", "Dr. B", "dept-01", 10),
                new AvailableDoctor("doc-3", "Dr. C", "dept-01", 5)));
        aiService.resultFuture = CompletableFuture.completedFuture(AiResult.success(aiData));

        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

        List<com.aimedical.modules.consultation.dto.RecommendedDoctor> doctors = result.getDoctors();
        assertEquals(3, doctors.size());
        assertTrue(doctors.get(0).getAvailableSlotCount() >= doctors.get(1).getAvailableSlotCount());
        assertTrue(doctors.get(1).getAvailableSlotCount() >= doctors.get(2).getAvailableSlotCount());
    }

    @Test
    void shouldReturnAllDoctorsWhenTotalIsLessThanFive() {
        DialogueCreateRequest request = createBasicRequest();
        TriageResponse aiData = createAiTriageResponse(VALID_UUID, "dept-01", "dept-02");
        doctorFacade.addDoctors("dept-01", List.of(
                new AvailableDoctor("doc-1", "Dr. A", "dept-01", 3)));
        doctorFacade.addDoctors("dept-02", List.of(
                new AvailableDoctor("doc-2", "Dr. B", "dept-02", 2)));
        aiService.resultFuture = CompletableFuture.completedFuture(AiResult.success(aiData));

        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

        assertEquals(2, result.getDoctors().size());
    }

    @Test
    void shouldSkipDepartmentOnDoctorFacadeException() {
        DialogueCreateRequest request = createBasicRequest();
        TriageResponse aiData = createAiTriageResponse(VALID_UUID, "dept-01", "dept-02");
        doctorFacade.addThrow("dept-01");
        doctorFacade.addDoctors("dept-02", List.of(
                new AvailableDoctor("doc-2", "Dr. B", "dept-02", 5)));
        aiService.resultFuture = CompletableFuture.completedFuture(AiResult.success(aiData));

        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

        assertEquals(1, result.getDoctors().size());
        assertEquals("doc-2", result.getDoctors().get(0).getDoctorId());

        assertEquals(1, logAppender.list.size());
        ILoggingEvent logEvent = logAppender.list.get(0);
        assertEquals(Level.WARN, logEvent.getLevel());
        String msg = logEvent.getFormattedMessage();
        assertTrue(msg.contains("DoctorFacade call failed for department dept-01"));
        assertTrue(msg.contains("ExecutionException"));
        assertTrue(msg.contains("DoctorFacade error"));
    }

    @Test
    void shouldReturnEmptyWhenAllDepartmentsThrow() {
        DialogueCreateRequest request = createBasicRequest();
        TriageResponse aiData = createAiTriageResponse(VALID_UUID, "dept-01", "dept-02");
        doctorFacade.addThrow("dept-01");
        doctorFacade.addThrow("dept-02");
        aiService.resultFuture = CompletableFuture.completedFuture(AiResult.success(aiData));

        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

        assertNotNull(result.getDoctors());
        assertTrue(result.getDoctors().isEmpty());

        assertEquals(2, logAppender.list.size());
        for (ILoggingEvent event : logAppender.list) {
            assertEquals(Level.WARN, event.getLevel());
            assertTrue(event.getFormattedMessage().contains("DoctorFacade call failed for department"));
            assertTrue(event.getFormattedMessage().contains("ExecutionException"));
        }
    }

    @Test
    void shouldKeepScoreAsZeroForAllMappedDoctors() {
        DialogueCreateRequest request = createBasicRequest();
        TriageResponse aiData = createAiTriageResponse(VALID_UUID, "dept-01", "dept-02");
        doctorFacade.addDoctors("dept-01", List.of(
                new AvailableDoctor("doc-1", "Dr. A", "dept-01", 5)));
        doctorFacade.addDoctors("dept-02", List.of(
                new AvailableDoctor("doc-2", "Dr. B", "dept-02", 3)));
        aiService.resultFuture = CompletableFuture.completedFuture(AiResult.success(aiData));

        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

        for (com.aimedical.modules.consultation.dto.RecommendedDoctor doc : result.getDoctors()) {
            assertEquals(0f, doc.getScore(), 0.001f);
        }
    }

    @Test
    void shouldReturnDoctorsOnFallbackPath() {
        DialogueCreateRequest request = createBasicRequest();
        aiService.resultFuture = CompletableFuture.completedFuture(AiResult.failure("AI_ERROR"));
        doctorFacade.addDoctors("dept-01", List.of(
                new AvailableDoctor("doc-1", "Dr. A", "dept-01", 5)));

        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

        assertTrue(result.isDegraded());
        assertEquals(1, result.getDoctors().size());
        assertEquals("doc-1", result.getDoctors().get(0).getDoctorId());
    }

    @Test
    void shouldSetRuleVersionMismatchOnFallbackResponse() {
        DialogueCreateRequest request = createBasicRequest();
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.failure("AI_ERROR"));
        ruleEngine.returnMismatch = true;

        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

        assertTrue(result.isDegraded());
        assertTrue(result.getRuleVersionMismatch());
    }

    @Test
    void shouldThrowWhenChiefComplaintAndAdditionalResponsesBothPresent() {
        DialogueCreateRequest request = createBasicRequest();
        request.setAdditionalResponses(List.of(
                new com.aimedical.modules.consultation.dto.AdditionalResponse("q?", "a", null)));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.triage(request));
        assertEquals(TriageErrorCode.TRIAGE_FIELD_COMBINATION_INVALID, ex.getErrorCode());
    }

    @Test
    void shouldThrowWhenChiefComplaintAndAdditionalResponsesBothAbsent() {
        DialogueCreateRequest request = new DialogueCreateRequest();
        request.setChiefComplaint("   ");
        request.setSessionId(VALID_UUID);
        request.setPatientId("P001");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.triage(request));
        assertEquals(TriageErrorCode.TRIAGE_FIELD_COMBINATION_INVALID, ex.getErrorCode());
    }

    @Test
    void shouldPassWhenOnlyChiefComplaintPresent() {
        DialogueCreateRequest request = createBasicRequest();
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiTriageResponse(VALID_UUID)));
        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);
        assertFalse(result.isDegraded());
    }

    @Test
    void shouldPassWhenOnlyAdditionalResponsesPresent() {
        DialogueCreateRequest request = new DialogueCreateRequest();
        request.setChiefComplaint("   ");
        request.setSessionId(VALID_UUID);
        request.setPatientId("P001");
        request.setAdditionalResponses(List.of(
                new com.aimedical.modules.consultation.dto.AdditionalResponse("q?", "a", null)));
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiTriageResponse(VALID_UUID)));
        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);
        assertFalse(result.isDegraded());
    }

    @Test
    void shouldThrowBusinessExceptionWhenSessionIdIsInvalidUuid() {
        DialogueCreateRequest request = createBasicRequest();
        request.setSessionId("not-a-uuid");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.triage(request));
        assertEquals(TriageErrorCode.TRIAGE_SESSION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void shouldCopyRuleVersionFromRequestToSessionWhenSessionNull() {
        DialogueCreateRequest request = createBasicRequest();
        request.setRuleVersion("v2.0");
        request.setRuleSetId("RS002");
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiTriageResponse(VALID_UUID)));

        service.triage(request);

        DialogueSession session = sessionStore.get(VALID_UUID);
        assertNotNull(session);
        assertEquals("v2.0", session.getRuleVersion());
        assertEquals("RS002", session.getRuleSetId());
    }

    @Test
    void shouldNotOverwriteSessionRuleVersionWhenAlreadySet() {
        DialogueCreateRequest firstRequest = createBasicRequest();
        firstRequest.setRuleVersion("v1.0");
        firstRequest.setRuleSetId("RS001");
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiTriageResponse(VALID_UUID)));
        service.triage(firstRequest);

        DialogueCreateRequest secondRequest = createBasicRequest();
        secondRequest.setRuleVersion("v2.0");
        secondRequest.setRuleSetId("RS002");
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiTriageResponse(VALID_UUID)));
        service.triage(secondRequest);

        DialogueSession session = sessionStore.get(VALID_UUID);
        assertEquals("v1.0", session.getRuleVersion());
        assertEquals("RS001", session.getRuleSetId());
    }

    @Test
    void shouldUseSessionRuleVersionInFallbackMatch() {
        DialogueCreateRequest request = createBasicRequest();
        request.setRuleVersion("v2.0");
        request.setRuleSetId("RS002");
        ruleEngine.capturedRuleVersion = null;
        ruleEngine.capturedRuleSetId = null;
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.failure("AI_ERROR"));

        service.triage(request);

        assertEquals("v2.0", ruleEngine.capturedRuleVersion);
        assertEquals("RS002", ruleEngine.capturedRuleSetId);
    }

    @Test
    void shouldUseChineseFallbackReason() {
        DialogueCreateRequest request = createBasicRequest();
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.failure("AI_ERROR"));

        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

        assertTrue(result.isDegraded());
        assertEquals("AI 服务不可用，已切换至规则引擎降级", result.getReason());
    }

    @Test
    void shouldUseChineseFallbackHintAfterMaxFailures() {
        DialogueCreateRequest request = createBasicRequest();
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.failure("AI_ERROR"));

        service.triage(request);
        service.triage(request);
        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

        assertEquals("AI 服务持续不可用，建议稍后重试", result.getFallbackHint());
    }

    @Test
    void shouldSkipDepartmentOnDoctorFacadeTimeout() {
        DialogueCreateRequest request = createBasicRequest();
        TriageResponse aiData = createAiTriageResponse(VALID_UUID, "dept-01");
        doctorFacade.addDoctors("dept-01", List.of(
                new AvailableDoctor("doc-1", "Dr. Wang", "dept-01", 10)));
        doctorFacade.addDelay("dept-01", 100L);
        aiService.resultFuture = CompletableFuture.completedFuture(AiResult.success(aiData));

        TriageServiceImpl timeoutService = new TriageServiceImpl(aiService, ruleEngine,
                fallbackProvider, doctorFacade, sessionManager, recordRepository,
                converter, objectMapper, transactionManager, 5L, 0L);

        com.aimedical.modules.consultation.dto.TriageResponse result = timeoutService.triage(request);

        assertTrue(result.getDoctors().isEmpty());

        assertEquals(1, logAppender.list.size());
        ILoggingEvent logEvent = logAppender.list.get(0);
        assertEquals(Level.WARN, logEvent.getLevel());
        assertTrue(logEvent.getFormattedMessage().contains("TimeoutException"));
    }

    @Test
    void shouldRestoreInterruptFlagOnDoctorFacadeInterrupt() {
        DialogueCreateRequest request = createBasicRequest();
        TriageResponse aiData = createAiTriageResponse(VALID_UUID, "dept-01");
        doctorFacade.addDoctors("dept-01", List.of(
                new AvailableDoctor("doc-1", "Dr. Wang", "dept-01", 10)));
        doctorFacade.addDelay("dept-01", 5000L);
        aiService.resultFuture = CompletableFuture.completedFuture(AiResult.success(aiData));

        Thread.currentThread().interrupt();
        try {
            com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

            assertTrue(result.getDoctors().isEmpty());
            assertTrue(Thread.interrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void shouldSkipOnlyFailedDepartmentOnMixedResults() {
        DialogueCreateRequest request = createBasicRequest();
        TriageResponse aiData = createAiTriageResponse(VALID_UUID, "dept-01", "dept-02");
        doctorFacade.addDoctors("dept-01", List.of(
                new AvailableDoctor("doc-1", "Dr. A", "dept-01", 5)));
        doctorFacade.addThrow("dept-02");
        aiService.resultFuture = CompletableFuture.completedFuture(AiResult.success(aiData));

        com.aimedical.modules.consultation.dto.TriageResponse result = service.triage(request);

        assertEquals(1, result.getDoctors().size());
        assertEquals("doc-1", result.getDoctors().get(0).getDoctorId());

        assertEquals(1, logAppender.list.size());
        ILoggingEvent logEvent = logAppender.list.get(0);
        assertEquals(Level.WARN, logEvent.getLevel());
        assertTrue(logEvent.getFormattedMessage().contains("ExecutionException"));
    }

    @Test
    void shouldHandleConcurrentSaveTriageRecord() throws InterruptedException {
        DialogueCreateRequest request = createBasicRequest();
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiTriageResponse(VALID_UUID)));

        int threadCount = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicReference<Throwable> error = new AtomicReference<>();

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    service.triage(request);
                } catch (Throwable t) {
                    error.set(t);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        assertNull(error.get(), "Concurrent saveTriageRecord should not throw");
        assertTrue(recordRepository.saved);
        assertNotNull(recordRepository.record);
        assertEquals(VALID_UUID, recordRepository.record.getSessionId());
    }

    @Test
    void shouldNotLeakLockEntries() throws Exception {
        int maxEntries = 1005;
        for (int i = 0; i < maxEntries; i++) {
            String sid = String.format("550e8400-e29b-41d4-a716-%012d", i);
            DialogueCreateRequest request = createBasicRequest();
            request.setSessionId(sid);
            TriageResponse aiData = createAiTriageResponse(sid);
            aiService.resultFuture = CompletableFuture.completedFuture(AiResult.success(aiData));
            service.triage(request);
        }

        java.lang.reflect.Field locksField = TriageServiceImpl.class.getDeclaredField("triageLocks");
        locksField.setAccessible(true);
        Map<?, ?> locks = (Map<?, ?>) locksField.get(service);
        assertTrue(locks.size() <= 1001, "triageLocks size=" + locks.size() + " should not exceed 1001");
    }

    @Test
    void shouldHandleConcurrentSaveTriageRecordWithDifferentSessionIds() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicReference<Throwable> error = new AtomicReference<>();

        for (int i = 0; i < threadCount; i++) {
            final String sid = String.format("660e8400-e29b-41d4-a716-%012d", i);
            new Thread(() -> {
                try {
                    DialogueCreateRequest req = createBasicRequest();
                    req.setSessionId(sid);
                    TriageResponse aiData = createAiTriageResponse(sid);
                    aiService.resultFuture = CompletableFuture.completedFuture(AiResult.success(aiData));
                    service.triage(req);
                } catch (Throwable t) {
                    error.set(t);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        assertNull(error.get(), "Concurrent saveTriageRecord with different sessionIds should not throw");
    }

    private DialogueCreateRequest createBasicRequest() {
        DialogueCreateRequest req = new DialogueCreateRequest();
        req.setChiefComplaint("头痛三天");
        req.setSessionId(VALID_UUID);
        req.setPatientId("P001");
        return req;
    }

    private TriageResponse createAiTriageResponse(String sessionId, String... deptIds) {
        if (deptIds.length == 0) {
            return createSingleDeptResponse(sessionId);
        }
        TriageResponse resp = new TriageResponse();
        resp.setSessionId(sessionId);
        resp.setReason("test reason");
        List<RecommendedDepartment> depts = new ArrayList<>();
        for (String deptId : deptIds) {
            depts.add(createAIDept(deptId));
        }
        resp.setRecommendedDepartments(depts);
        return resp;
    }

    private static RecommendedDepartment createAIDept(String deptId) {
        RecommendedDepartment dept = new RecommendedDepartment();
        dept.setDepartmentId(deptId);
        dept.setDepartmentName("Name-" + deptId);
        dept.setScore(0.5f);
        return dept;
    }

    private TriageResponse createSingleDeptResponse(String sessionId) {
        TriageResponse resp = new TriageResponse();
        resp.setSessionId(sessionId);
        resp.setReason("test reason");
        List<RecommendedDepartment> depts = new ArrayList<>();
        RecommendedDepartment dept = new RecommendedDepartment();
        dept.setDepartmentId("dept-01");
        dept.setDepartmentName("神经内科");
        dept.setScore(0.9f);
        depts.add(dept);
        resp.setRecommendedDepartments(depts);
        return resp;
    }

    private static class StubAiService implements AiService {
        CompletableFuture<AiResult<TriageResponse>> resultFuture;

        @Override
        public CompletableFuture<AiResult<TriageResponse>> triage(
                com.aimedical.modules.ai.api.dto.triage.TriageRequest request) {
            return resultFuture;
        }

        @Override
        public CompletableFuture<AiResult<com.aimedical.modules.ai.api.dto.diagnosis.DiagnosisResponse>> diagnosis(
                com.aimedical.modules.ai.api.dto.diagnosis.DiagnosisRequest request) { return null; }

        @Override
        public CompletableFuture<AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionCheckResponse>> prescriptionCheck(
                com.aimedical.modules.ai.api.dto.prescription.PrescriptionCheckRequest request) { return null; }

        @Override
        public CompletableFuture<AiResult<com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordGenResponse>> generateMedicalRecord(
                com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordGenRequest request) { return null; }

        @Override
        public CompletableFuture<AiResult<com.aimedical.modules.ai.api.dto.inspection.InspectionReportResponse>> analysisReportForInspection(
                com.aimedical.modules.ai.api.dto.inspection.InspectionReportRequest request) { return null; }

        @Override
        public CompletableFuture<AiResult<com.aimedical.modules.ai.api.dto.labtest.LabTestReportResponse>> analysisReportForLabTest(
                com.aimedical.modules.ai.api.dto.labtest.LabTestReportRequest request) { return null; }

        @Override
        public CompletableFuture<AiResult<com.aimedical.modules.ai.api.dto.image.ImageAnalysisResponse>> imageAnalysis(
                com.aimedical.modules.ai.api.dto.image.ImageAnalysisRequest request) { return null; }

        @Override
        public CompletableFuture<AiResult<com.aimedical.modules.ai.api.dto.kb.KbQueryResponse>> knowledgeBaseQuery(
                com.aimedical.modules.ai.api.dto.kb.KbQueryRequest request) { return null; }

        @Override
        public CompletableFuture<AiResult<com.aimedical.modules.ai.api.dto.examination.ExaminationRecommendResponse>> recommendExamination(
                com.aimedical.modules.ai.api.dto.examination.ExaminationRecommendRequest request) { return null; }

        @Override
        public CompletableFuture<AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse>> prescriptionAssist(
                com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistRequest request) { return null; }

        @Override
        public CompletableFuture<AiResult<com.aimedical.modules.ai.api.dto.execution.ExecutionOrderResponse>> recommendExecutionOrder(
                com.aimedical.modules.ai.api.dto.execution.ExecutionOrderRequest request) { return null; }

        @Override
        public CompletableFuture<AiResult<com.aimedical.modules.ai.api.dto.schedule.ScheduleResponse>> schedule(
                com.aimedical.modules.ai.api.dto.schedule.ScheduleRequest request) { return null; }

        @Override
        public CompletableFuture<AiResult<com.aimedical.modules.ai.api.dto.discussion.DiscussionConclusionResponse>> discussionConclusion(
                com.aimedical.modules.ai.api.dto.discussion.DiscussionConclusionRequest request) { return null; }
    }

    private static class StubTriageRuleEngine implements TriageRuleEngine {
        boolean returnEmpty = false;
        boolean returnMismatch = false;
        String capturedRuleVersion;
        String capturedRuleSetId;

        @Override
        public MatchResult match(
                String chiefComplaint, String ruleVersion, String ruleSetId) {
            this.capturedRuleVersion = ruleVersion;
            this.capturedRuleSetId = ruleSetId;
            if (returnEmpty) return new MatchResult(Collections.emptyList(), returnMismatch);
            List<com.aimedical.modules.consultation.dto.RecommendedDepartment> list = new ArrayList<>();
            list.add(new com.aimedical.modules.consultation.dto.RecommendedDepartment("dept-01", "神经内科", 0.9f));
            return new MatchResult(list, returnMismatch);
        }

        @Override
        public String currentRuleVersion() { return "v1"; }

        @Override
        public String currentRuleSetId() { return "RS001"; }
    }

    private static class StubFallbackProvider implements DepartmentFallbackProvider {
        boolean returnEmpty = false;

        @Override
        public List<com.aimedical.modules.consultation.dto.RecommendedDepartment> getFallbackDepartments() {
            if (returnEmpty) {
                return Collections.emptyList();
            }
            List<com.aimedical.modules.consultation.dto.RecommendedDepartment> list = new ArrayList<>();
            list.add(new com.aimedical.modules.consultation.dto.RecommendedDepartment("fallback-dept-id", "内科", 0f));
            return list;
        }
    }

    private static class StubDoctorFacade implements DoctorFacade {
        final Map<String, List<AvailableDoctor>> doctorsByDept = new HashMap<>();
        final Set<String> throwOnDept = new HashSet<>();
        final Map<String, Long> delayMs = new HashMap<>();
        final Map<String, CountDownLatch> blockingReadyLatch = new HashMap<>();
        RuntimeException exceptionToThrow = new RuntimeException("DoctorFacade error");

        void addDoctors(String departmentId, List<AvailableDoctor> doctors) {
            doctorsByDept.put(departmentId, doctors);
        }

        void addThrow(String departmentId) {
            throwOnDept.add(departmentId);
        }

        void addDelay(String departmentId, long millis) {
            delayMs.put(departmentId, millis);
        }

        void addBlocking(String departmentId, CountDownLatch readyLatch) {
            blockingReadyLatch.put(departmentId, readyLatch);
        }

        @Override
        public List<AvailableDoctor> findAvailableDoctorsByDepartment(String departmentId) {
            if (throwOnDept.contains(departmentId)) {
                throw exceptionToThrow;
            }
            if (blockingReadyLatch.containsKey(departmentId)) {
                blockingReadyLatch.get(departmentId).countDown();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (delayMs.containsKey(departmentId)) {
                try {
                    Thread.sleep(delayMs.get(departmentId));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return doctorsByDept.getOrDefault(departmentId, Collections.emptyList());
        }
    }

    private static class StubTransactionManager implements PlatformTransactionManager {
        boolean getTransactionCalled = false;
        boolean commitCalled = false;
        boolean rollbackCalled = false;

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            getTransactionCalled = true;
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
            commitCalled = true;
        }

        @Override
        public void rollback(TransactionStatus status) {
            rollbackCalled = true;
        }

        void reset() {
            getTransactionCalled = false;
            commitCalled = false;
            rollbackCalled = false;
        }
    }

    private static class StubSessionStore implements SessionStore<String, DialogueSession> {
        private final java.util.Map<String, DialogueSession> map = new java.util.HashMap<>();

        @Override
        public void put(String key, DialogueSession value) { map.put(key, value); }

        @Override
        public DialogueSession get(String key) { return map.get(key); }

        @Override
        public DialogueSession remove(String key) { return map.remove(key); }

        @Override
        public boolean containsKey(String key) { return map.containsKey(key); }

        @Override
        public Set<String> keySet() { return map.keySet(); }
    }

    private static class StubTriageRecordRepository implements TriageRecordRepository {
        TriageRecord record;
        boolean saved = false;
        boolean throwOnSave = false;

        @Override
        public Optional<TriageRecord> findBySessionId(String sessionId) {
            if (record != null && sessionId.equals(record.getSessionId())) {
                return Optional.of(record);
            }
            return Optional.empty();
        }

        @Override
        public Optional<TriageRecord> findTopBySessionIdOrderByTriageTimeDesc(String sessionId) {
            if (record != null && sessionId.equals(record.getSessionId())) {
                return Optional.of(record);
            }
            return Optional.empty();
        }

        @Override
        public TriageRecord save(TriageRecord entity) {
            saved = true;
            if (throwOnSave) {
                throw new RuntimeException("Simulated DB error");
            }
            this.record = entity;
            return entity;
        }

        @Override
        public Optional<TriageRecord> findTopByPatientIdOrderByTriageTimeDesc(String patientId) {
            return Optional.empty();
        }

        @Override
        public List<TriageRecord> findBySessionIdIn(List<String> sessionIds) {
            return Collections.emptyList();
        }

        @Override
        public List<TriageRecord> findAll() { return Collections.emptyList(); }

        @Override
        public List<TriageRecord> findAll(Sort sort) { return Collections.emptyList(); }

        @Override
        public List<TriageRecord> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }

        @Override
        public <S extends TriageRecord> List<S> saveAll(Iterable<S> entities) { return null; }

        @Override
        public void flush() {}

        @Override
        public <S extends TriageRecord> S saveAndFlush(S entity) { return entity; }

        @Override
        public <S extends TriageRecord> List<S> saveAllAndFlush(Iterable<S> entities) { return null; }

        @Override
        public void deleteAllInBatch(Iterable<TriageRecord> entities) {}

        @Override
        public void deleteAllByIdInBatch(Iterable<Long> longs) {}

        @Override
        public void deleteAllInBatch() {}

        @Override
        public TriageRecord getOne(Long aLong) { return null; }

        @Override
        public TriageRecord getById(Long aLong) { return null; }

        @Override
        public TriageRecord getReferenceById(Long aLong) { return null; }

        @Override
        public <S extends TriageRecord> Optional<S> findOne(Example<S> example) { return Optional.empty(); }

        @Override
        public <S extends TriageRecord> List<S> findAll(Example<S> example) { return null; }

        @Override
        public <S extends TriageRecord> List<S> findAll(Example<S> example, Sort sort) { return null; }

        @Override
        public <S extends TriageRecord> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }

        @Override
        public <S extends TriageRecord> long count(Example<S> example) { return 0; }

        @Override
        public <S extends TriageRecord> boolean exists(Example<S> example) { return false; }

        @Override
        public <S extends TriageRecord, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }

        @Override
        public Optional<TriageRecord> findById(Long aLong) { return Optional.empty(); }

        @Override
        public boolean existsById(Long aLong) { return false; }

        @Override
        public long count() { return 0; }

        @Override
        public void deleteById(Long aLong) {}

        @Override
        public void delete(TriageRecord entity) {}

        @Override
        public void deleteAllById(Iterable<? extends Long> longs) {}

        @Override
        public void deleteAll(Iterable<? extends TriageRecord> entities) {}

        @Override
        public void deleteAll() {}

        @Override
        public Page<TriageRecord> findAll(Pageable pageable) { return null; }
    }
}
