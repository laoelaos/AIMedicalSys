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
import com.aimedical.modules.ai.api.dto.triage.RecommendedDoctor;
import com.aimedical.modules.ai.api.dto.triage.TriageRequest;
import com.aimedical.modules.ai.api.dto.triage.TriageResponse;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "ai.mock.enabled", havingValue = "true", matchIfMissing = true)
public class MockAiService implements AiService {

    @Override
    public CompletableFuture<AiResult<TriageResponse>> triage(TriageRequest request) {
        TriageResponse response = new TriageResponse();
        response.setSessionId(request.getSessionId() != null
                ? request.getSessionId()
                : UUID.randomUUID().toString());

        if (request.getChiefComplaint() != null
                && request.getChiefComplaint().startsWith("degraded:")) {
            response.setComplete(true);
            response.setDegraded(true);
            response.setReason("AI 服务繁忙，已降级为常见分诊规则推荐");
            return CompletableFuture.completedFuture(
                    new AiResult<>(false, response, null, true, response.getReason()));
        }

        if (request.getAdditionalResponses() == null || request.getAdditionalResponses().isEmpty()) {
            response.setComplete(false);
            response.setQuestion("请问这个症状持续多久了？有没有其他伴随症状？之前是否因此就医或服用过药物？");
        } else {
            response.setComplete(true);
            RecommendedDepartment dept = new RecommendedDepartment();
            dept.setDepartmentId(1);
            dept.setDepartmentName("神经内科");
            dept.setScore(92);
            response.setDepartments(List.of(dept));

            RecommendedDoctor doc = new RecommendedDoctor();
            doc.setDoctorId(101);
            doc.setDoctorName("王主任");
            doc.setAvailableSlotCount(5);
            doc.setScore(95);
            response.setDoctors(List.of(doc));
            response.setReason("根据主诉综合分析，建议优先就诊神经内科以排除相关疾病");
        }
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
