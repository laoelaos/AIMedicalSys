package com.aimedical.modules.ai.impl.mock;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.aimedical.modules.ai.api.AiResult;
import com.aimedical.modules.ai.api.AiService;
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
import com.aimedical.modules.ai.api.dto.triage.RecommendedDepartment;
import com.aimedical.modules.ai.api.dto.triage.TriageRequest;
import com.aimedical.modules.ai.api.dto.triage.TriageResponse;

@Service
@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true", matchIfMissing = true)
public class MockAiService implements AiService {

    @Override
    public CompletableFuture<AiResult<TriageResponse>> triage(TriageRequest request) {
        RecommendedDepartment dept = new RecommendedDepartment();
        dept.setDepartmentName("mock_departmentName");
        TriageResponse response = new TriageResponse();
        response.setRecommendedDepartments(List.of(dept));
        response.setReason("mock_reason");
        return CompletableFuture.completedFuture(AiResult.success(response));
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
}
