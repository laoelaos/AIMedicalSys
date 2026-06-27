package com.aimedical.modules.ai.impl.fallback;

import org.junit.jupiter.api.Test;

import com.aimedical.modules.ai.api.AiResult;
import com.aimedical.modules.ai.api.AiService;
import com.aimedical.modules.ai.api.degradation.DegradationContext;
import com.aimedical.modules.ai.api.degradation.DegradationStrategy;
import com.aimedical.modules.ai.api.dto.diagnosis.DiagnosisRequest;
import com.aimedical.modules.ai.api.dto.diagnosis.DiagnosisResponse;
import com.aimedical.modules.ai.api.dto.discussion.DiscussionConclusionRequest;
import com.aimedical.modules.ai.api.dto.discussion.DiscussionConclusionResponse;
import com.aimedical.modules.ai.api.dto.examination.ExaminationRecommendRequest;
import com.aimedical.modules.ai.api.dto.examination.ExaminationRecommendResponse;
import com.aimedical.modules.ai.api.dto.execution.ExecutionOrderRequest;
import com.aimedical.modules.ai.api.dto.execution.ExecutionOrderResponse;
import com.aimedical.modules.ai.api.dto.image.ImageAnalysisRequest;
import com.aimedical.modules.ai.api.dto.image.ImageAnalysisResponse;
import com.aimedical.modules.ai.api.dto.inspection.InspectionReportRequest;
import com.aimedical.modules.ai.api.dto.inspection.InspectionReportResponse;
import com.aimedical.modules.ai.api.dto.kb.KbQueryRequest;
import com.aimedical.modules.ai.api.dto.kb.KbQueryResponse;
import com.aimedical.modules.ai.api.dto.labtest.LabTestReportRequest;
import com.aimedical.modules.ai.api.dto.labtest.LabTestReportResponse;
import com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordGenRequest;
import com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordGenResponse;
import com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistRequest;
import com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse;
import com.aimedical.modules.ai.api.dto.prescription.PrescriptionCheckRequest;
import com.aimedical.modules.ai.api.dto.prescription.PrescriptionCheckResponse;
import com.aimedical.modules.ai.api.dto.schedule.ScheduleRequest;
import com.aimedical.modules.ai.api.dto.schedule.ScheduleResponse;
import com.aimedical.modules.ai.api.dto.triage.TriageRequest;
import com.aimedical.modules.ai.api.dto.triage.TriageResponse;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.LoggerFactory;

class FallbackAiServiceTest {

    @Test
    void shouldDelegateToFirstAvailableService() {
        AiService delegate = mock(AiService.class);
        TriageRequest request = new TriageRequest();
        TriageResponse response = new TriageResponse();
        when(delegate.triage(request)).thenReturn(CompletableFuture.completedFuture(AiResult.success(response)));

        FallbackAiService fallback = new FallbackAiService(List.of(delegate), List.of());
        AiResult<TriageResponse> result = fallback.triage(request).join();

        assertTrue(result.isSuccess());
        assertSame(response, result.getData());
    }

    @Test
    void shouldReturnFallbackResultWhenNoDelegateAvailable() {
        FallbackAiService fallback = new FallbackAiService(List.of(), List.of());
        AiResult<TriageResponse> result = fallback.triage(new TriageRequest()).join();

        assertFalse(result.isSuccess());
        assertTrue(result.isDegraded());
        assertEquals("No available AiService delegate", result.getFallbackReason());
    }

    @Test
    void shouldDegradeWhenStrategyTriggers() {
        AiService delegate = mock(AiService.class);
        TriageRequest request = new TriageRequest();
        AiResult<TriageResponse> failureResult = AiResult.failure("ERR");
        when(delegate.triage(request)).thenReturn(CompletableFuture.completedFuture(failureResult));

        DegradationStrategy strategy = mock(DegradationStrategy.class);
        when(strategy.shouldDegrade(any())).thenReturn(true);

        FallbackAiService fallback = new FallbackAiService(List.of(delegate), List.of(strategy));
        AiResult<TriageResponse> result = fallback.triage(request).join();

        assertFalse(result.isSuccess());
        assertTrue(result.isDegraded());
        assertEquals("Degraded by strategy", result.getFallbackReason());
    }

    @Test
    void shouldReturnOriginalResultWhenNoStrategyDegrades() {
        AiService delegate = mock(AiService.class);
        TriageRequest request = new TriageRequest();
        AiResult<TriageResponse> failureResult = AiResult.failure("ERR");
        when(delegate.triage(request)).thenReturn(CompletableFuture.completedFuture(failureResult));

        DegradationStrategy strategy = mock(DegradationStrategy.class);
        when(strategy.shouldDegrade(any())).thenReturn(false);

        FallbackAiService fallback = new FallbackAiService(List.of(delegate), List.of(strategy));
        AiResult<TriageResponse> result = fallback.triage(request).join();

        assertFalse(result.isSuccess());
        assertFalse(result.isDegraded());
        assertEquals("ERR", result.getErrorCode());
    }

    @Test
    void shouldExcludeSelfFromDelegates() {
        AiService delegate = mock(AiService.class);
        FallbackAiService outer = new FallbackAiService(List.of(delegate), List.of());
        FallbackAiService inner = new FallbackAiService(List.of(outer, delegate), List.of());

        TriageRequest request = new TriageRequest();
        TriageResponse response = new TriageResponse();
        when(delegate.triage(request)).thenReturn(CompletableFuture.completedFuture(AiResult.success(response)));

        AiResult<TriageResponse> result = inner.triage(request).join();
        assertTrue(result.isSuccess());
    }

    @Test
    void shouldReturnOriginalResultWhenDelegateAlreadyDegraded() {
        AiService delegate = mock(AiService.class);
        TriageRequest request = new TriageRequest();
        AiResult<TriageResponse> degradedResult = AiResult.degraded("already degraded");
        when(delegate.triage(request)).thenReturn(CompletableFuture.completedFuture(degradedResult));

        FallbackAiService fallback = new FallbackAiService(List.of(delegate), List.of());
        AiResult<TriageResponse> result = fallback.triage(request).join();

        assertFalse(result.isSuccess());
        assertTrue(result.isDegraded());
        assertEquals("already degraded", result.getFallbackReason());
    }

    @Test
    void shouldLogErrorOnConstruction() {
        Logger logger = (Logger) LoggerFactory.getLogger(FallbackAiService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            new FallbackAiService(List.of(), List.of());
            assertEquals(1, appender.list.size());
            assertEquals(Level.ERROR, appender.list.get(0).getLevel());
            assertEquals("No available AiService delegate", appender.list.get(0).getFormattedMessage());
        } finally {
            appender.stop();
            logger.detachAppender(appender);
        }
    }

    @Test
    void shouldLogWarnOnSubsequentCalls() {
        Logger logger = (Logger) LoggerFactory.getLogger(FallbackAiService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            FallbackAiService service = new FallbackAiService(List.of(), List.of());

            service.triage(new TriageRequest()).join();
            assertEquals(2, appender.list.size());
            assertEquals(Level.WARN, appender.list.get(1).getLevel());
            assertEquals("No available AiService delegate", appender.list.get(1).getFormattedMessage());

            appender.list.clear();

            service.triage(new TriageRequest()).join();
            assertEquals(1, appender.list.size());
            assertEquals(Level.WARN, appender.list.get(0).getLevel());
            assertEquals("No available AiService delegate", appender.list.get(0).getFormattedMessage());
        } finally {
            appender.stop();
            logger.detachAppender(appender);
        }
    }

    @Test
    void diagnosisShouldDelegateWhenAvailable() {
        AiService delegate = mock(AiService.class);
        DiagnosisRequest request = new DiagnosisRequest();
        DiagnosisResponse response = new DiagnosisResponse();
        when(delegate.diagnosis(request)).thenReturn(CompletableFuture.completedFuture(AiResult.success(response)));

        FallbackAiService fallback = new FallbackAiService(List.of(delegate), List.of());
        AiResult<DiagnosisResponse> result = fallback.diagnosis(request).join();

        assertTrue(result.isSuccess());
        assertSame(response, result.getData());
    }

    @Test
    void diagnosisShouldReturnDegradedWhenNoDelegate() {
        FallbackAiService fallback = new FallbackAiService(List.of(), List.of());
        AiResult<DiagnosisResponse> result = fallback.diagnosis(new DiagnosisRequest()).join();

        assertTrue(result.isDegraded());
    }

    @Test
    void prescriptionCheckShouldDelegateWhenAvailable() {
        AiService delegate = mock(AiService.class);
        PrescriptionCheckRequest request = new PrescriptionCheckRequest();
        PrescriptionCheckResponse response = new PrescriptionCheckResponse();
        when(delegate.prescriptionCheck(request)).thenReturn(CompletableFuture.completedFuture(AiResult.success(response)));

        FallbackAiService fallback = new FallbackAiService(List.of(delegate), List.of());
        AiResult<PrescriptionCheckResponse> result = fallback.prescriptionCheck(request).join();

        assertTrue(result.isSuccess());
        assertSame(response, result.getData());
    }

    @Test
    void prescriptionCheckShouldReturnDegradedWhenNoDelegate() {
        FallbackAiService fallback = new FallbackAiService(List.of(), List.of());
        AiResult<PrescriptionCheckResponse> result = fallback.prescriptionCheck(new PrescriptionCheckRequest()).join();

        assertTrue(result.isDegraded());
    }

    @Test
    void generateMedicalRecordShouldDelegateWhenAvailable() {
        AiService delegate = mock(AiService.class);
        MedicalRecordGenRequest request = new MedicalRecordGenRequest();
        MedicalRecordGenResponse response = new MedicalRecordGenResponse();
        when(delegate.generateMedicalRecord(request)).thenReturn(CompletableFuture.completedFuture(AiResult.success(response)));

        FallbackAiService fallback = new FallbackAiService(List.of(delegate), List.of());
        AiResult<MedicalRecordGenResponse> result = fallback.generateMedicalRecord(request).join();

        assertTrue(result.isSuccess());
        assertSame(response, result.getData());
    }

    @Test
    void generateMedicalRecordShouldReturnDegradedWhenNoDelegate() {
        FallbackAiService fallback = new FallbackAiService(List.of(), List.of());
        AiResult<MedicalRecordGenResponse> result = fallback.generateMedicalRecord(new MedicalRecordGenRequest()).join();

        assertTrue(result.isDegraded());
    }

    @Test
    void analysisReportForInspectionShouldDelegateWhenAvailable() {
        AiService delegate = mock(AiService.class);
        InspectionReportRequest request = new InspectionReportRequest();
        InspectionReportResponse response = new InspectionReportResponse();
        when(delegate.analysisReportForInspection(request)).thenReturn(CompletableFuture.completedFuture(AiResult.success(response)));

        FallbackAiService fallback = new FallbackAiService(List.of(delegate), List.of());
        AiResult<InspectionReportResponse> result = fallback.analysisReportForInspection(request).join();

        assertTrue(result.isSuccess());
        assertSame(response, result.getData());
    }

    @Test
    void analysisReportForInspectionShouldReturnDegradedWhenNoDelegate() {
        FallbackAiService fallback = new FallbackAiService(List.of(), List.of());
        AiResult<InspectionReportResponse> result = fallback.analysisReportForInspection(new InspectionReportRequest()).join();

        assertTrue(result.isDegraded());
    }

    @Test
    void analysisReportForLabTestShouldDelegateWhenAvailable() {
        AiService delegate = mock(AiService.class);
        LabTestReportRequest request = new LabTestReportRequest();
        LabTestReportResponse response = new LabTestReportResponse();
        when(delegate.analysisReportForLabTest(request)).thenReturn(CompletableFuture.completedFuture(AiResult.success(response)));

        FallbackAiService fallback = new FallbackAiService(List.of(delegate), List.of());
        AiResult<LabTestReportResponse> result = fallback.analysisReportForLabTest(request).join();

        assertTrue(result.isSuccess());
        assertSame(response, result.getData());
    }

    @Test
    void analysisReportForLabTestShouldReturnDegradedWhenNoDelegate() {
        FallbackAiService fallback = new FallbackAiService(List.of(), List.of());
        AiResult<LabTestReportResponse> result = fallback.analysisReportForLabTest(new LabTestReportRequest()).join();

        assertTrue(result.isDegraded());
    }

    @Test
    void imageAnalysisShouldDelegateWhenAvailable() {
        AiService delegate = mock(AiService.class);
        ImageAnalysisRequest request = new ImageAnalysisRequest();
        ImageAnalysisResponse response = new ImageAnalysisResponse();
        when(delegate.imageAnalysis(request)).thenReturn(CompletableFuture.completedFuture(AiResult.success(response)));

        FallbackAiService fallback = new FallbackAiService(List.of(delegate), List.of());
        AiResult<ImageAnalysisResponse> result = fallback.imageAnalysis(request).join();

        assertTrue(result.isSuccess());
        assertSame(response, result.getData());
    }

    @Test
    void imageAnalysisShouldReturnDegradedWhenNoDelegate() {
        FallbackAiService fallback = new FallbackAiService(List.of(), List.of());
        AiResult<ImageAnalysisResponse> result = fallback.imageAnalysis(new ImageAnalysisRequest()).join();

        assertTrue(result.isDegraded());
    }

    @Test
    void knowledgeBaseQueryShouldDelegateWhenAvailable() {
        AiService delegate = mock(AiService.class);
        KbQueryRequest request = new KbQueryRequest();
        KbQueryResponse response = new KbQueryResponse();
        when(delegate.knowledgeBaseQuery(request)).thenReturn(CompletableFuture.completedFuture(AiResult.success(response)));

        FallbackAiService fallback = new FallbackAiService(List.of(delegate), List.of());
        AiResult<KbQueryResponse> result = fallback.knowledgeBaseQuery(request).join();

        assertTrue(result.isSuccess());
        assertSame(response, result.getData());
    }

    @Test
    void knowledgeBaseQueryShouldReturnDegradedWhenNoDelegate() {
        FallbackAiService fallback = new FallbackAiService(List.of(), List.of());
        AiResult<KbQueryResponse> result = fallback.knowledgeBaseQuery(new KbQueryRequest()).join();

        assertTrue(result.isDegraded());
    }

    @Test
    void recommendExaminationShouldDelegateWhenAvailable() {
        AiService delegate = mock(AiService.class);
        ExaminationRecommendRequest request = new ExaminationRecommendRequest();
        ExaminationRecommendResponse response = new ExaminationRecommendResponse();
        when(delegate.recommendExamination(request)).thenReturn(CompletableFuture.completedFuture(AiResult.success(response)));

        FallbackAiService fallback = new FallbackAiService(List.of(delegate), List.of());
        AiResult<ExaminationRecommendResponse> result = fallback.recommendExamination(request).join();

        assertTrue(result.isSuccess());
        assertSame(response, result.getData());
    }

    @Test
    void recommendExaminationShouldReturnDegradedWhenNoDelegate() {
        FallbackAiService fallback = new FallbackAiService(List.of(), List.of());
        AiResult<ExaminationRecommendResponse> result = fallback.recommendExamination(new ExaminationRecommendRequest()).join();

        assertTrue(result.isDegraded());
    }

    @Test
    void prescriptionAssistShouldDelegateWhenAvailable() {
        AiService delegate = mock(AiService.class);
        PrescriptionAssistRequest request = new PrescriptionAssistRequest();
        PrescriptionAssistResponse response = new PrescriptionAssistResponse();
        when(delegate.prescriptionAssist(request)).thenReturn(CompletableFuture.completedFuture(AiResult.success(response)));

        FallbackAiService fallback = new FallbackAiService(List.of(delegate), List.of());
        AiResult<PrescriptionAssistResponse> result = fallback.prescriptionAssist(request).join();

        assertTrue(result.isSuccess());
        assertSame(response, result.getData());
    }

    @Test
    void prescriptionAssistShouldReturnDegradedWhenNoDelegate() {
        FallbackAiService fallback = new FallbackAiService(List.of(), List.of());
        AiResult<PrescriptionAssistResponse> result = fallback.prescriptionAssist(new PrescriptionAssistRequest()).join();

        assertTrue(result.isDegraded());
    }

    @Test
    void recommendExecutionOrderShouldDelegateWhenAvailable() {
        AiService delegate = mock(AiService.class);
        ExecutionOrderRequest request = new ExecutionOrderRequest();
        ExecutionOrderResponse response = new ExecutionOrderResponse();
        when(delegate.recommendExecutionOrder(request)).thenReturn(CompletableFuture.completedFuture(AiResult.success(response)));

        FallbackAiService fallback = new FallbackAiService(List.of(delegate), List.of());
        AiResult<ExecutionOrderResponse> result = fallback.recommendExecutionOrder(request).join();

        assertTrue(result.isSuccess());
        assertSame(response, result.getData());
    }

    @Test
    void recommendExecutionOrderShouldReturnDegradedWhenNoDelegate() {
        FallbackAiService fallback = new FallbackAiService(List.of(), List.of());
        AiResult<ExecutionOrderResponse> result = fallback.recommendExecutionOrder(new ExecutionOrderRequest()).join();

        assertTrue(result.isDegraded());
    }

    @Test
    void scheduleShouldDelegateWhenAvailable() {
        AiService delegate = mock(AiService.class);
        ScheduleRequest request = new ScheduleRequest();
        ScheduleResponse response = new ScheduleResponse();
        when(delegate.schedule(request)).thenReturn(CompletableFuture.completedFuture(AiResult.success(response)));

        FallbackAiService fallback = new FallbackAiService(List.of(delegate), List.of());
        AiResult<ScheduleResponse> result = fallback.schedule(request).join();

        assertTrue(result.isSuccess());
        assertSame(response, result.getData());
    }

    @Test
    void scheduleShouldReturnDegradedWhenNoDelegate() {
        FallbackAiService fallback = new FallbackAiService(List.of(), List.of());
        AiResult<ScheduleResponse> result = fallback.schedule(new ScheduleRequest()).join();

        assertTrue(result.isDegraded());
    }

    @Test
    void discussionConclusionShouldDelegateWhenAvailable() {
        AiService delegate = mock(AiService.class);
        DiscussionConclusionRequest request = new DiscussionConclusionRequest();
        DiscussionConclusionResponse response = new DiscussionConclusionResponse();
        when(delegate.discussionConclusion(request)).thenReturn(CompletableFuture.completedFuture(AiResult.success(response)));

        FallbackAiService fallback = new FallbackAiService(List.of(delegate), List.of());
        AiResult<DiscussionConclusionResponse> result = fallback.discussionConclusion(request).join();

        assertTrue(result.isSuccess());
        assertSame(response, result.getData());
    }

    @Test
    void discussionConclusionShouldReturnDegradedWhenNoDelegate() {
        FallbackAiService fallback = new FallbackAiService(List.of(), List.of());
        AiResult<DiscussionConclusionResponse> result = fallback.discussionConclusion(new DiscussionConclusionRequest()).join();

        assertTrue(result.isDegraded());
    }
}
