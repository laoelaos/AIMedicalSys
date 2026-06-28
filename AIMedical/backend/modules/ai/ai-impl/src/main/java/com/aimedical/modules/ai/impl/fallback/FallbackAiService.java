package com.aimedical.modules.ai.impl.fallback;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

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

/**
 * AI 服务降级包装实现。
 *
 * <p>采用 {@code @Primary} + 委托模式：注入所有非自身的 {@link AiService} 实现，
 * 取第一个作为主委托；调用后经 {@link DegradationStrategy} 链判定是否降级。
 *
 * <p>设计说明（T11）：
 * <ul>
 *   <li>{@code @Primary} 确保 DoctorAiServiceImpl 注入的是本类而非具体实现，
 *       形成双重锁定降级（本类策略判定 + DoctorAiServiceImpl 兜底捕获）。</li>
 *   <li>后续引入真实 AI 实现时，只需新增 {@code @Service} AiService Bean，
 *       本类自动将其纳入 delegates，无需修改 DoctorAiServiceImpl。</li>
 *   <li>当前 delegates 仅取 {@code get(0)}，多实现并存时的路由策略待扩展。</li>
 * </ul>
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Primary
@Service
public class FallbackAiService implements AiService {

    private static final Logger log = LoggerFactory.getLogger(FallbackAiService.class);

    private final List<AiService> delegates;
    private final List<DegradationStrategy> strategies;

    public FallbackAiService(List<AiService> aiServiceList,
                             List<DegradationStrategy> strategies) {
        this.delegates = aiServiceList.stream()
                .filter(s -> !(s instanceof FallbackAiService))
                .collect(Collectors.toList());
        this.strategies = strategies;
        if (this.delegates.isEmpty()) {
            log.error("No available AiService delegate");
        }
    }

    private <T> CompletableFuture<AiResult<T>> handleEmptyDelegates() {
        log.warn("No available AiService delegate");
        return CompletableFuture.completedFuture(AiResult.degraded("No available AiService delegate"));
    }

    @Override
    public CompletableFuture<AiResult<TriageResponse>> triage(TriageRequest request) {
        if (delegates.isEmpty()) {
            return handleEmptyDelegates();
        }
        return delegates.get(0).triage(request)
                .thenApply(this::applyStrategies);
    }

    @Override
    public CompletableFuture<AiResult<DiagnosisResponse>> diagnosis(DiagnosisRequest request) {
        if (delegates.isEmpty()) {
            return handleEmptyDelegates();
        }
        return delegates.get(0).diagnosis(request)
                .thenApply(this::applyStrategies);
    }

    @Override
    public CompletableFuture<AiResult<PrescriptionCheckResponse>> prescriptionCheck(PrescriptionCheckRequest request) {
        if (delegates.isEmpty()) {
            return handleEmptyDelegates();
        }
        return delegates.get(0).prescriptionCheck(request)
                .thenApply(this::applyStrategies);
    }

    @Override
    public CompletableFuture<AiResult<MedicalRecordGenResponse>> generateMedicalRecord(MedicalRecordGenRequest request) {
        if (delegates.isEmpty()) {
            return handleEmptyDelegates();
        }
        return delegates.get(0).generateMedicalRecord(request)
                .thenApply(this::applyStrategies);
    }

    @Override
    public CompletableFuture<AiResult<InspectionReportResponse>> analysisReportForInspection(InspectionReportRequest request) {
        if (delegates.isEmpty()) {
            return handleEmptyDelegates();
        }
        return delegates.get(0).analysisReportForInspection(request)
                .thenApply(this::applyStrategies);
    }

    @Override
    public CompletableFuture<AiResult<LabTestReportResponse>> analysisReportForLabTest(LabTestReportRequest request) {
        if (delegates.isEmpty()) {
            return handleEmptyDelegates();
        }
        return delegates.get(0).analysisReportForLabTest(request)
                .thenApply(this::applyStrategies);
    }

    @Override
    public CompletableFuture<AiResult<ImageAnalysisResponse>> imageAnalysis(ImageAnalysisRequest request) {
        if (delegates.isEmpty()) {
            return handleEmptyDelegates();
        }
        return delegates.get(0).imageAnalysis(request)
                .thenApply(this::applyStrategies);
    }

    @Override
    public CompletableFuture<AiResult<KbQueryResponse>> knowledgeBaseQuery(KbQueryRequest request) {
        if (delegates.isEmpty()) {
            return handleEmptyDelegates();
        }
        return delegates.get(0).knowledgeBaseQuery(request)
                .thenApply(this::applyStrategies);
    }

    @Override
    public CompletableFuture<AiResult<ExaminationRecommendResponse>> recommendExamination(ExaminationRecommendRequest request) {
        if (delegates.isEmpty()) {
            return handleEmptyDelegates();
        }
        return delegates.get(0).recommendExamination(request)
                .thenApply(this::applyStrategies);
    }

    @Override
    public CompletableFuture<AiResult<PrescriptionAssistResponse>> prescriptionAssist(PrescriptionAssistRequest request) {
        if (delegates.isEmpty()) {
            return handleEmptyDelegates();
        }
        return delegates.get(0).prescriptionAssist(request)
                .thenApply(this::applyStrategies);
    }

    @Override
    public CompletableFuture<AiResult<ExecutionOrderResponse>> recommendExecutionOrder(ExecutionOrderRequest request) {
        if (delegates.isEmpty()) {
            return handleEmptyDelegates();
        }
        return delegates.get(0).recommendExecutionOrder(request)
                .thenApply(this::applyStrategies);
    }

    @Override
    public CompletableFuture<AiResult<ScheduleResponse>> schedule(ScheduleRequest request) {
        if (delegates.isEmpty()) {
            return handleEmptyDelegates();
        }
        return delegates.get(0).schedule(request)
                .thenApply(this::applyStrategies);
    }

    @Override
    public CompletableFuture<AiResult<DiscussionConclusionResponse>> discussionConclusion(DiscussionConclusionRequest request) {
        if (delegates.isEmpty()) {
            return handleEmptyDelegates();
        }
        return delegates.get(0).discussionConclusion(request)
                .thenApply(this::applyStrategies);
    }

    private <T> AiResult<T> applyStrategies(AiResult<T> result) {
        if (result.isSuccess() || result.isDegraded()) {
            return result;
        }
        DegradationContext context = new DegradationContext();
        for (DegradationStrategy strategy : strategies) {
            if (strategy.shouldDegrade(context)) {
                return AiResult.degraded("Degraded by strategy");
            }
        }
        return result;
    }
}
