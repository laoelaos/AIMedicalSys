package com.aimedical.modules.medicalrecord.service.impl;

import com.aimedical.modules.ai.api.AiResult;
import com.aimedical.modules.ai.api.AiService;
import com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordGenRequest;
import com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordGenResponse;
import com.aimedical.modules.commonmodule.visit.VisitFacade;
import com.aimedical.modules.medicalrecord.converter.MedicalRecordConverter;
import com.aimedical.modules.medicalrecord.detector.MissingFieldDetector;
import com.aimedical.modules.medicalrecord.dto.FieldMissingHint;
import com.aimedical.modules.medicalrecord.dto.RecordGenerateRequest;
import com.aimedical.modules.medicalrecord.dto.RecordGenerateResponse;
import com.aimedical.modules.medicalrecord.entity.MedicalRecord;
import com.aimedical.modules.medicalrecord.enums.MedicalRecordField;
import com.aimedical.modules.medicalrecord.exception.MedicalRecordErrorCode;
import com.aimedical.modules.medicalrecord.repository.MedicalRecordRepository;
import com.aimedical.modules.medicalrecord.template.DepartmentTemplateConfig;
import com.aimedical.modules.medicalrecord.template.TemplateConfigManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class MedicalRecordServiceImplTest {

    private StubVisitFacade visitFacade;
    private StubTemplateConfigManager templateManager;
    private StubAiService aiService;
    private StubMissingFieldDetector detector;
    private MedicalRecordConverter converter;
    private StubMedicalRecordRepository recordRepository;
    private ExecutorService medicalRecordExecutor;
    private MedicalRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        visitFacade = new StubVisitFacade();
        templateManager = new StubTemplateConfigManager();
        aiService = new StubAiService();
        detector = new StubMissingFieldDetector();
        converter = new MedicalRecordConverter(new ObjectMapper());
        recordRepository = new StubMedicalRecordRepository();
        medicalRecordExecutor = new SameThreadExecutor();
        service = new MedicalRecordServiceImpl(visitFacade, templateManager, aiService,
                detector, converter, recordRepository, medicalRecordExecutor);
        ReflectionTestUtils.setField(service, "aiTimeout", 12);
        ReflectionTestUtils.setField(service, "visitFacadeTimeout", 2);
    }

    @Test
    void shouldReturnVisitNotFoundWhenEncounterIdIsNull() {
        RecordGenerateRequest request = createRequest();
        request.setEncounterId(null);

        RecordGenerateResponse response = service.generate(request);

        assertFalse(response.isSuccess());
        assertEquals(MedicalRecordErrorCode.MR_GEN_VISIT_NOT_FOUND, response.getErrorCode());
        assertTrue(response.getFields().isEmpty());
        assertTrue(response.getMissingFieldHints().isEmpty());
    }

    @Test
    void shouldReturnVisitNotFoundWhenEncounterIdIsEmpty() {
        RecordGenerateRequest request = createRequest();
        request.setEncounterId("");

        RecordGenerateResponse response = service.generate(request);

        assertFalse(response.isSuccess());
        assertEquals(MedicalRecordErrorCode.MR_GEN_VISIT_NOT_FOUND, response.getErrorCode());
    }

    @Test
    void shouldUseFallbackWhenVisitFacadeTimesOut() {
        visitFacade.timeout = true;
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiResponse()));

        RecordGenerateRequest request = createRequest();
        request.setEncounterId("E001");

        RecordGenerateResponse response = service.generate(request);

        assertTrue(response.isSuccess());
        assertTrue(response.isFromFallback());
    }

    @Test
    void shouldUseFallbackWhenVisitFacadeThrowsException() {
        visitFacade.throwException = true;
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiResponse()));

        RecordGenerateRequest request = createRequest();
        request.setEncounterId("E001");

        RecordGenerateResponse response = service.generate(request);

        assertTrue(response.isSuccess());
        assertTrue(response.isFromFallback());
    }

    @Test
    void shouldReturnDegradedWhenAiTimesOut() {
        visitFacade.returnValue = "V001";
        aiService.resultFuture = CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("timeout");
        });

        RecordGenerateRequest request = createRequest();
        request.setEncounterId("E001");

        RecordGenerateResponse response = service.generate(request);

        assertFalse(response.isSuccess());
        assertTrue(response.isDegraded());
        assertEquals(MedicalRecordErrorCode.MR_GEN_AI_EXECUTION_ERROR, response.getErrorCode());
    }

    @Test
    void shouldReturnInterruptedOnInterruptedException() {
        visitFacade.returnValue = "V001";
        aiService.resultFuture = new CompletableFuture<>();

        Thread.currentThread().interrupt();
        try {
            RecordGenerateRequest request = createRequest();
            request.setEncounterId("E001");

            RecordGenerateResponse response = service.generate(request);

            assertFalse(response.isSuccess());
            assertTrue(response.isDegraded());
            assertEquals(MedicalRecordErrorCode.MR_GEN_AI_INTERRUPTED, response.getErrorCode());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void shouldReturnExecutionErrorOnExecutionException() {
        visitFacade.returnValue = "V001";
        CompletableFuture<AiResult<MedicalRecordGenResponse>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("AI error"));
        aiService.resultFuture = future;

        RecordGenerateRequest request = createRequest();
        request.setEncounterId("E001");

        RecordGenerateResponse response = service.generate(request);

        assertFalse(response.isSuccess());
        assertTrue(response.isDegraded());
    }

    @Test
    void shouldReturnSuccessOnNormalFlow() {
        visitFacade.returnValue = "V001";
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiResponse()));

        RecordGenerateRequest request = createRequest();
        RecordGenerateResponse response = service.generate(request);

        assertTrue(response.isSuccess());
        assertFalse(response.isDegraded());
        assertFalse(response.isFromFallback());
        assertEquals(7, response.getFields().size());
    }

    @Test
    void shouldSaveMedicalRecordOnSuccess() {
        visitFacade.returnValue = "V001";
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiResponse()));

        RecordGenerateRequest request = createRequest();
        service.generate(request);

        assertTrue(recordRepository.saved);
    }

    @Test
    void shouldHandleOptimisticLockException() {
        visitFacade.returnValue = "V001";
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiResponse()));
        recordRepository.throwOptimisticLock = true;

        RecordGenerateRequest request = createRequest();
        RecordGenerateResponse response = service.generate(request);

        assertTrue(response.isSuccess());
        assertEquals(MedicalRecordErrorCode.MR_GEN_CONCURRENT_MODIFICATION, response.getErrorCode());
    }

    @Test
    void shouldHandleDataIntegrityViolationException() {
        visitFacade.returnValue = "V001";
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiResponse()));
        recordRepository.throwDataIntegrityViolation = true;

        RecordGenerateRequest request = createRequest();
        RecordGenerateResponse response = service.generate(request);

        assertTrue(response.isSuccess());
        assertEquals(MedicalRecordErrorCode.MR_GEN_CONCURRENT_MODIFICATION, response.getErrorCode());
    }

    @Test
    void shouldReuseExistingRecordOnUpdatePath() {
        visitFacade.returnValue = "V001";
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiResponse()));
        MedicalRecord existing = new MedicalRecord();
        existing.setRecordId(1L);
        existing.setVersion(1);
        recordRepository.existingRecord = existing;

        RecordGenerateRequest request = createRequest();
        service.generate(request);

        assertEquals(existing, recordRepository.savedEntity);
        assertEquals(Integer.valueOf(1), existing.getVersion());
    }

    @Test
    void shouldCreateNewRecordOnInsertPath() {
        visitFacade.returnValue = "V001";
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiResponse()));
        recordRepository.existingRecord = null;

        RecordGenerateRequest request = createRequest();
        service.generate(request);

        assertNotNull(recordRepository.savedEntity);
        assertNull(recordRepository.savedEntity.getRecordId());
    }

    @Test
    void shouldWriteDoctorIdToEntity() {
        visitFacade.returnValue = "V001";
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiResponse()));

        RecordGenerateRequest request = createRequest();
        request.setDoctorId("D001");
        service.generate(request);

        assertEquals("D001", recordRepository.savedEntity.getDoctorId());
    }

    @Test
    void shouldDetectMissingFields() {
        visitFacade.returnValue = "V001";
        MedicalRecordGenResponse aiResp = new MedicalRecordGenResponse();
        aiResp.setChiefComplaint("头痛");
        aiResp.setSymptomDescription(null);
        aiService.resultFuture = CompletableFuture.completedFuture(AiResult.success(aiResp));

        RecordGenerateRequest request = createRequest();
        RecordGenerateResponse response = service.generate(request);

        assertTrue(response.isSuccess());
        assertNotNull(response.getMissingFieldHints());
    }

    @Test
    void shouldReturnHintsFromDetector() {
        visitFacade.returnValue = "V001";
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiResponse()));
        detector.hints = List.of(new FieldMissingHint());

        RecordGenerateRequest request = createRequest();
        RecordGenerateResponse response = service.generate(request);

        assertEquals(detector.hints, response.getMissingFieldHints());
    }

    @Test
    void shouldSetVisitIdFallbackWhenEncounterIdFallbackUsed() {
        visitFacade.throwException = true;
        aiService.resultFuture = CompletableFuture.completedFuture(
                AiResult.success(createAiResponse()));

        RecordGenerateRequest request = createRequest();
        request.setEncounterId("E001");
        RecordGenerateResponse response = service.generate(request);

        assertTrue(response.isFromFallback());
    }

    private static RecordGenerateRequest createRequest() {
        RecordGenerateRequest req = new RecordGenerateRequest();
        req.setDialogueText("对话内容");
        req.setPatientId("P001");
        req.setEncounterId("E001");
        req.setDepartmentId("dept-01");
        return req;
    }

    private static MedicalRecordGenResponse createAiResponse() {
        MedicalRecordGenResponse resp = new MedicalRecordGenResponse();
        resp.setChiefComplaint("头痛");
        resp.setSymptomDescription("跳痛");
        resp.setPresentIllness("3天");
        resp.setPastHistory("无");
        resp.setPhysicalExam("正常");
        resp.setPreliminaryDiagnosis("偏头痛");
        resp.setTreatmentPlan("休息");
        return resp;
    }

    private static class StubVisitFacade implements VisitFacade {
        String returnValue;
        boolean timeout;
        boolean throwException;

        @Override
        public String findVisitIdByEncounterId(String encounterId) {
            if (timeout) {
                throw new RuntimeException("模拟超时");
            }
            if (throwException) {
                throw new RuntimeException("模拟异常");
            }
            return returnValue;
        }
    }

    private static class StubTemplateConfigManager implements TemplateConfigManager {
        @Override
        public DepartmentTemplateConfig getTemplate(String departmentId) {
            Set<MedicalRecordField> allFields = Set.of(MedicalRecordField.values());
            return new DepartmentTemplateConfig(departmentId, allFields, Collections.emptyMap(), Collections.emptyMap());
        }
    }

    private static class StubAiService implements AiService {
        CompletableFuture<AiResult<MedicalRecordGenResponse>> resultFuture;

        @Override
        public CompletableFuture<AiResult<MedicalRecordGenResponse>> generateMedicalRecord(MedicalRecordGenRequest request) {
            return resultFuture;
        }

        @Override
        public CompletableFuture<AiResult<com.aimedical.modules.ai.api.dto.triage.TriageResponse>> triage(
                com.aimedical.modules.ai.api.dto.triage.TriageRequest request) { return null; }
        @Override
        public CompletableFuture<AiResult<com.aimedical.modules.ai.api.dto.diagnosis.DiagnosisResponse>> diagnosis(
                com.aimedical.modules.ai.api.dto.diagnosis.DiagnosisRequest request) { return null; }
        @Override
        public CompletableFuture<AiResult<com.aimedical.modules.ai.api.dto.prescription.PrescriptionCheckResponse>> prescriptionCheck(
                com.aimedical.modules.ai.api.dto.prescription.PrescriptionCheckRequest request) { return null; }
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

    private static class StubMissingFieldDetector implements MissingFieldDetector {
        List<FieldMissingHint> hints = Collections.emptyList();

        @Override
        public List<FieldMissingHint> detect(
                com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordGenResponse aiResponse,
                DepartmentTemplateConfig template) {
            return hints;
        }
    }

    private static class StubMedicalRecordRepository implements MedicalRecordRepository {
        boolean saved;
        boolean throwOptimisticLock;
        boolean throwDataIntegrityViolation;
        MedicalRecord existingRecord;
        MedicalRecord savedEntity;

        @Override
        public MedicalRecord save(MedicalRecord entity) {
            if (throwOptimisticLock) {
                throw new ObjectOptimisticLockingFailureException(MedicalRecord.class, 1L);
            }
            if (throwDataIntegrityViolation) {
                throw new DataIntegrityViolationException("Duplicate entry for visitId");
            }
            saved = true;
            savedEntity = entity;
            return entity;
        }

        @Override
        public Optional<MedicalRecord> findByVisitId(String visitId) {
            return Optional.ofNullable(existingRecord);
        }
        @Override
        public Optional<MedicalRecord> findByPatientId(String patientId) { return Optional.empty(); }
        @Override
        public java.util.List<MedicalRecord> findByVisitIdFallbackTrue() { return java.util.Collections.emptyList(); }
        @Override
        public Optional<MedicalRecord> findById(Long id) { return Optional.empty(); }
        @Override
        public boolean existsById(Long id) { return false; }
        @Override
        public java.util.List<MedicalRecord> findAll() { return Collections.emptyList(); }
        @Override
        public java.util.List<MedicalRecord> findAllById(Iterable<Long> ids) { return Collections.emptyList(); }
        @Override
        public long count() { return 0; }
        @Override
        public void deleteById(Long id) {}
        @Override
        public void delete(MedicalRecord entity) {}
        @Override
        public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override
        public void deleteAll(Iterable<? extends MedicalRecord> entities) {}
        @Override
        public void deleteAll() {}
        @Override
        public void flush() {}
        @Override
        public <S extends MedicalRecord> S saveAndFlush(S entity) { return entity; }
        @Override
        public <S extends MedicalRecord> java.util.List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override
        public void deleteAllInBatch(Iterable<MedicalRecord> entities) {}
        @Override
        public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override
        public void deleteAllInBatch() {}
        @Override
        public MedicalRecord getOne(Long id) { return null; }
        @Override
        public MedicalRecord getById(Long id) { return null; }
        @Override
        public MedicalRecord getReferenceById(Long id) { return null; }
        @Override
        public <S extends MedicalRecord> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override
        public <S extends MedicalRecord> java.util.List<S> findAll(org.springframework.data.domain.Example<S> example) { return null; }
        @Override
        public <S extends MedicalRecord> java.util.List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return null; }
        @Override
        public <S extends MedicalRecord> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return null; }
        @Override
        public <S extends MedicalRecord> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override
        public <S extends MedicalRecord> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override
        public <S extends MedicalRecord, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override
        public java.util.List<MedicalRecord> findAll(org.springframework.data.domain.Sort sort) { return Collections.emptyList(); }
        @Override
        public org.springframework.data.domain.Page<MedicalRecord> findAll(org.springframework.data.domain.Pageable pageable) { return null; }
        @Override
        public <S extends MedicalRecord> java.util.List<S> saveAll(Iterable<S> entities) { return null; }
    }

    private static class SameThreadExecutor implements ExecutorService {
        @Override
        public void execute(Runnable command) {
            command.run();
        }

        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return true;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            try {
                return CompletableFuture.completedFuture(task.call());
            } catch (Exception e) {
                CompletableFuture<T> f = new CompletableFuture<>();
                f.completeExceptionally(e);
                return f;
            }
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            task.run();
            return CompletableFuture.completedFuture(result);
        }

        @Override
        public Future<?> submit(Runnable task) {
            task.run();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
            List<Future<T>> futures = new ArrayList<>(tasks.size());
            for (Callable<T> task : tasks) {
                futures.add(submit(task));
            }
            return futures;
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            return invokeAll(tasks);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws ExecutionException {
            try {
                return tasks.iterator().next().call();
            } catch (Exception e) {
                throw new ExecutionException(e);
            }
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws ExecutionException, TimeoutException {
            return invokeAny(tasks);
        }
    }
}
