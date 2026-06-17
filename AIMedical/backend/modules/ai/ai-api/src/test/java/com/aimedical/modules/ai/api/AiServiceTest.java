package com.aimedical.modules.ai.api;

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
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class AiServiceTest {

    private final AiService service = new AiService() {
        @Override
        public CompletableFuture<AiResult<TriageResponse>> triage(TriageRequest request) {
            return CompletableFuture.completedFuture(AiResult.success(new TriageResponse()));
        }

        @Override
        public CompletableFuture<AiResult<DiagnosisResponse>> diagnosis(DiagnosisRequest request) {
            return CompletableFuture.completedFuture(AiResult.success(new DiagnosisResponse()));
        }

        @Override
        public CompletableFuture<AiResult<PrescriptionCheckResponse>> prescriptionCheck(PrescriptionCheckRequest request) {
            return CompletableFuture.completedFuture(AiResult.success(new PrescriptionCheckResponse()));
        }

        @Override
        public CompletableFuture<AiResult<MedicalRecordGenResponse>> generateMedicalRecord(MedicalRecordGenRequest request) {
            return CompletableFuture.completedFuture(AiResult.success(new MedicalRecordGenResponse()));
        }

        @Override
        public CompletableFuture<AiResult<InspectionReportResponse>> analysisReportForInspection(InspectionReportRequest request) {
            return CompletableFuture.completedFuture(AiResult.success(new InspectionReportResponse()));
        }

        @Override
        public CompletableFuture<AiResult<LabTestReportResponse>> analysisReportForLabTest(LabTestReportRequest request) {
            return CompletableFuture.completedFuture(AiResult.success(new LabTestReportResponse()));
        }

        @Override
        public CompletableFuture<AiResult<ImageAnalysisResponse>> imageAnalysis(ImageAnalysisRequest request) {
            return CompletableFuture.completedFuture(AiResult.success(new ImageAnalysisResponse()));
        }

        @Override
        public CompletableFuture<AiResult<KbQueryResponse>> knowledgeBaseQuery(KbQueryRequest request) {
            return CompletableFuture.completedFuture(AiResult.success(new KbQueryResponse()));
        }

        @Override
        public CompletableFuture<AiResult<ExaminationRecommendResponse>> recommendExamination(ExaminationRecommendRequest request) {
            return CompletableFuture.completedFuture(AiResult.success(new ExaminationRecommendResponse()));
        }

        @Override
        public CompletableFuture<AiResult<PrescriptionAssistResponse>> prescriptionAssist(PrescriptionAssistRequest request) {
            return CompletableFuture.completedFuture(AiResult.success(new PrescriptionAssistResponse()));
        }

        @Override
        public CompletableFuture<AiResult<ExecutionOrderResponse>> recommendExecutionOrder(ExecutionOrderRequest request) {
            return CompletableFuture.completedFuture(AiResult.success(new ExecutionOrderResponse()));
        }

        @Override
        public CompletableFuture<AiResult<ScheduleResponse>> schedule(ScheduleRequest request) {
            return CompletableFuture.completedFuture(AiResult.success(new ScheduleResponse()));
        }

        @Override
        public CompletableFuture<AiResult<DiscussionConclusionResponse>> discussionConclusion(DiscussionConclusionRequest request) {
            return CompletableFuture.completedFuture(AiResult.success(new DiscussionConclusionResponse()));
        }
    };

    @Test
    void shouldImplementAll13Methods() {
        assertNotNull(service);
    }

    @Test
    void triageShouldReturnCompletableFuture() {
        CompletableFuture<AiResult<TriageResponse>> future = service.triage(new TriageRequest());
        assertNotNull(future);
        assertTrue(future.isDone());
        assertTrue(future.join().isSuccess());
    }

    @Test
    void diagnosisShouldReturnCompletableFuture() {
        CompletableFuture<AiResult<DiagnosisResponse>> future = service.diagnosis(new DiagnosisRequest());
        assertNotNull(future);
        assertTrue(future.isDone());
        assertTrue(future.join().isSuccess());
    }

    @Test
    void prescriptionCheckShouldReturnCompletableFuture() {
        CompletableFuture<AiResult<PrescriptionCheckResponse>> future = service.prescriptionCheck(new PrescriptionCheckRequest());
        assertNotNull(future);
        assertTrue(future.join().isSuccess());
    }

    @Test
    void generateMedicalRecordShouldReturnCompletableFuture() {
        CompletableFuture<AiResult<MedicalRecordGenResponse>> future = service.generateMedicalRecord(new MedicalRecordGenRequest());
        assertNotNull(future);
        assertTrue(future.join().isSuccess());
    }

    @Test
    void analysisReportForInspectionShouldReturnCompletableFuture() {
        CompletableFuture<AiResult<InspectionReportResponse>> future = service.analysisReportForInspection(new InspectionReportRequest());
        assertNotNull(future);
        assertTrue(future.join().isSuccess());
    }

    @Test
    void analysisReportForLabTestShouldReturnCompletableFuture() {
        CompletableFuture<AiResult<LabTestReportResponse>> future = service.analysisReportForLabTest(new LabTestReportRequest());
        assertNotNull(future);
        assertTrue(future.join().isSuccess());
    }

    @Test
    void imageAnalysisShouldReturnCompletableFuture() {
        CompletableFuture<AiResult<ImageAnalysisResponse>> future = service.imageAnalysis(new ImageAnalysisRequest());
        assertNotNull(future);
        assertTrue(future.join().isSuccess());
    }

    @Test
    void knowledgeBaseQueryShouldReturnCompletableFuture() {
        CompletableFuture<AiResult<KbQueryResponse>> future = service.knowledgeBaseQuery(new KbQueryRequest());
        assertNotNull(future);
        assertTrue(future.join().isSuccess());
    }

    @Test
    void recommendExaminationShouldReturnCompletableFuture() {
        CompletableFuture<AiResult<ExaminationRecommendResponse>> future = service.recommendExamination(new ExaminationRecommendRequest());
        assertNotNull(future);
        assertTrue(future.join().isSuccess());
    }

    @Test
    void prescriptionAssistShouldReturnCompletableFuture() {
        CompletableFuture<AiResult<PrescriptionAssistResponse>> future = service.prescriptionAssist(new PrescriptionAssistRequest());
        assertNotNull(future);
        assertTrue(future.join().isSuccess());
    }

    @Test
    void recommendExecutionOrderShouldReturnCompletableFuture() {
        CompletableFuture<AiResult<ExecutionOrderResponse>> future = service.recommendExecutionOrder(new ExecutionOrderRequest());
        assertNotNull(future);
        assertTrue(future.join().isSuccess());
    }

    @Test
    void scheduleShouldReturnCompletableFuture() {
        CompletableFuture<AiResult<ScheduleResponse>> future = service.schedule(new ScheduleRequest());
        assertNotNull(future);
        assertTrue(future.join().isSuccess());
    }

    @Test
    void discussionConclusionShouldReturnCompletableFuture() {
        CompletableFuture<AiResult<DiscussionConclusionResponse>> future = service.discussionConclusion(new DiscussionConclusionRequest());
        assertNotNull(future);
        assertTrue(future.join().isSuccess());
    }
}
