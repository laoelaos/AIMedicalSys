package com.aimedical.modules.doctor.service.impl;

import com.aimedical.common.result.Result;
import com.aimedical.modules.ai.api.AiResult;
import com.aimedical.modules.ai.api.AiService;
import com.aimedical.modules.ai.api.dto.diagnosis.DiagnosisResponse;
import com.aimedical.modules.ai.api.dto.examination.ExaminationRecommendResponse;
import com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordGenResponse;
import com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistResponse;
import com.aimedical.modules.ai.api.dto.prescription.PrescriptionCheckResponse;
import com.aimedical.modules.doctor.dto.request.AiDiagnosisRequest;
import com.aimedical.modules.doctor.dto.request.AiExaminationRequest;
import com.aimedical.modules.doctor.dto.request.AiMedicalRecordGenRequest;
import com.aimedical.modules.doctor.dto.request.AiPrescriptionAssistRequest;
import com.aimedical.modules.doctor.dto.request.AiPrescriptionAuditRequest;
import com.aimedical.modules.doctor.dto.response.AiDiagnosisResponse;
import com.aimedical.modules.doctor.dto.response.AiExaminationResponse;
import com.aimedical.modules.doctor.dto.response.AiMedicalRecordGenResponse;
import com.aimedical.modules.doctor.dto.response.AiPrescriptionAssistResponse;
import com.aimedical.modules.doctor.dto.response.AiPrescriptionAuditResponse;
import com.aimedical.modules.doctor.dto.response.AiResultResponse;
import com.aimedical.modules.doctor.entity.AiRiskLevel;
import com.aimedical.modules.doctor.service.DoctorAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 医生端 AI 服务实现（带降级包装）。
 *
 * <p>降级策略：
 * <ul>
 *   <li>当 {@code ai.doctor.mock-degrade=true}（默认）时，直接返回降级结果与兜底数据，
 *       用于演示 AI 不可用时的降级 UI 路径</li>
 *   <li>当 {@code ai.doctor.mock-degrade=false} 时，实际调用 {@link AiService}；
 *       调用异常或返回降级结果时，统一包装为 {@link AiResultResponse#degraded}</li>
 * </ul>
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Service
public class DoctorAiServiceImpl implements DoctorAiService {

    private static final Logger log = LoggerFactory.getLogger(DoctorAiServiceImpl.class);

    private final AiService aiService;

    @Value("${ai.doctor.mock-degrade:true}")
    private boolean mockDegrade;

    public DoctorAiServiceImpl(AiService aiService) {
        this.aiService = aiService;
    }

    @Override
    public Result<AiResultResponse<AiDiagnosisResponse>> diagnosis(AiDiagnosisRequest request, Long doctorUserId) {
        if (mockDegrade) {
            return Result.success(degradedDiagnosis());
        }
        try {
            AiResult<DiagnosisResponse> result = aiService.diagnosis(
                    new com.aimedical.modules.ai.api.dto.diagnosis.DiagnosisRequest(
                            request.patientId(), request.chiefComplaint(),
                            request.presentIllness(), request.pastHistory())).join();
            if (result.isDegraded() || !result.isSuccess()) {
                return Result.success(degradedDiagnosis());
            }
            DiagnosisResponse aiData = result.getData();
            if (aiData == null) {
                return Result.success(degradedDiagnosis());
            }
            AiDiagnosisResponse data = new AiDiagnosisResponse(
                    aiData.getPossibleDiagnoses() != null ? aiData.getPossibleDiagnoses() : List.of(),
                    aiData.getSummary() != null ? aiData.getSummary() : "");
            return Result.success(AiResultResponse.ok(data));
        } catch (Exception e) {
            log.warn("AI diagnosis 调用异常，降级处理", e);
            return Result.success(degradedDiagnosis());
        }
    }

    @Override
    public Result<AiResultResponse<AiExaminationResponse>> recommendExamination(AiExaminationRequest request, Long doctorUserId) {
        if (mockDegrade) {
            return Result.success(degradedExamination());
        }
        try {
            AiResult<ExaminationRecommendResponse> result = aiService.recommendExamination(
                    new com.aimedical.modules.ai.api.dto.examination.ExaminationRecommendRequest(
                            request.patientId(), request.diagnosis(), request.chiefComplaint())).join();
            if (result.isDegraded() || !result.isSuccess()) {
                return Result.success(degradedExamination());
            }
            ExaminationRecommendResponse aiData = result.getData();
            if (aiData == null) {
                return Result.success(degradedExamination());
            }
            List<AiExaminationResponse.ExaminationItem> items = aiData.getItems() == null
                    ? List.of()
                    : aiData.getItems().stream()
                            .map(i -> new AiExaminationResponse.ExaminationItem(i.getName(), i.getCategory(), i.getReason()))
                            .toList();
            AiExaminationResponse data = new AiExaminationResponse(items);
            return Result.success(AiResultResponse.ok(data));
        } catch (Exception e) {
            log.warn("AI recommendExamination 调用异常，降级处理", e);
            return Result.success(degradedExamination());
        }
    }

    @Override
    public Result<AiResultResponse<AiPrescriptionAssistResponse>> prescriptionAssist(AiPrescriptionAssistRequest request, Long doctorUserId) {
        if (mockDegrade) {
            return Result.success(degradedPrescriptionAssist());
        }
        try {
            com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistRequest aiReq =
                    new com.aimedical.modules.ai.api.dto.prescription.PrescriptionAssistRequest();
            aiReq.setDiagnosis(request.diagnosis());
            aiReq.setPrescriptionId(request.patientId() != null ? String.valueOf(request.patientId()) : null);
            AiResult<PrescriptionAssistResponse> result = aiService.prescriptionAssist(aiReq).join();
            if (result.isDegraded() || !result.isSuccess()) {
                return Result.success(degradedPrescriptionAssist());
            }
            PrescriptionAssistResponse aiData = result.getData();
            if (aiData == null) {
                return Result.success(degradedPrescriptionAssist());
            }
            AiPrescriptionAssistResponse data = new AiPrescriptionAssistResponse(List.of(),
                    aiData.getPrescriptionDraft() != null ? aiData.getPrescriptionDraft() : "");
            return Result.success(AiResultResponse.ok(data));
        } catch (Exception e) {
            log.warn("AI prescriptionAssist 调用异常，降级处理", e);
            return Result.success(degradedPrescriptionAssist());
        }
    }

    @Override
    public Result<AiResultResponse<AiPrescriptionAuditResponse>> prescriptionAudit(AiPrescriptionAuditRequest request, Long doctorUserId) {
        if (mockDegrade) {
            return Result.success(degradedPrescriptionAudit());
        }
        try {
            com.aimedical.modules.ai.api.dto.prescription.PrescriptionCheckRequest aiReq =
                    new com.aimedical.modules.ai.api.dto.prescription.PrescriptionCheckRequest();
            aiReq.setPrescriptionId(request.prescriptionId() != null ? String.valueOf(request.prescriptionId()) : null);
            AiResult<PrescriptionCheckResponse> result = aiService.prescriptionCheck(aiReq).join();
            if (result.isDegraded() || !result.isSuccess()) {
                return Result.success(degradedPrescriptionAudit());
            }
            PrescriptionCheckResponse aiData = result.getData();
            if (aiData == null) {
                return Result.success(degradedPrescriptionAudit());
            }
            List<String> warnings = aiData.getAlerts() == null
                    ? List.of()
                    : aiData.getAlerts().stream()
                            .map(a -> a.getAlertMessage() != null ? a.getAlertMessage() : "")
                            .toList();
            AiPrescriptionAuditResponse data = new AiPrescriptionAuditResponse(
                    parseRiskLevel(aiData.getRiskLevel()),
                    warnings,
                    !aiData.isFromFallback());
            return Result.success(AiResultResponse.ok(data));
        } catch (Exception e) {
            log.warn("AI prescriptionCheck 调用异常，降级处理", e);
            return Result.success(degradedPrescriptionAudit());
        }
    }

    @Override
    public Result<AiResultResponse<AiMedicalRecordGenResponse>> generateMedicalRecord(AiMedicalRecordGenRequest request, Long doctorUserId) {
        if (mockDegrade) {
            return Result.success(degradedMedicalRecordGen());
        }
        try {
            com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordGenRequest aiReq =
                    new com.aimedical.modules.ai.api.dto.medicalrecord.MedicalRecordGenRequest();
            aiReq.setPatientId(request.patientId() != null ? String.valueOf(request.patientId()) : null);
            StringBuilder dialogue = new StringBuilder();
            if (request.chiefComplaint() != null) dialogue.append("主诉: ").append(request.chiefComplaint()).append("\n");
            if (request.presentIllness() != null) dialogue.append("现病史: ").append(request.presentIllness()).append("\n");
            if (request.pastHistory() != null) dialogue.append("既往史: ").append(request.pastHistory()).append("\n");
            if (request.diagnosis() != null) dialogue.append("诊断: ").append(request.diagnosis());
            aiReq.setDialogueText(dialogue.toString());
            AiResult<MedicalRecordGenResponse> result = aiService.generateMedicalRecord(aiReq).join();
            if (result.isDegraded() || !result.isSuccess()) {
                return Result.success(degradedMedicalRecordGen());
            }
            MedicalRecordGenResponse aiData = result.getData();
            if (aiData == null) {
                return Result.success(degradedMedicalRecordGen());
            }
            AiMedicalRecordGenResponse data = new AiMedicalRecordGenResponse(
                    nullToEmpty(aiData.getChiefComplaint()),
                    nullToEmpty(aiData.getPresentIllness()),
                    nullToEmpty(aiData.getPastHistory()),
                    nullToEmpty(aiData.getPreliminaryDiagnosis()),
                    nullToEmpty(aiData.getTreatmentPlan()));
            return Result.success(AiResultResponse.ok(data));
        } catch (Exception e) {
            log.warn("AI generateMedicalRecord 调用异常，降级处理", e);
            return Result.success(degradedMedicalRecordGen());
        }
    }

    // ---------- 工具方法 ----------

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    /**
     * 将 AI 返回的风险等级字符串解析为 {@link AiRiskLevel} 枚举，
     * 无法匹配时返回 null（表示未知风险，由前端/审核人人工判断）。
     */
    private static AiRiskLevel parseRiskLevel(String code) {
        if (code == null) {
            return null;
        }
        for (AiRiskLevel level : AiRiskLevel.values()) {
            if (level.getCode().equals(code)) {
                return level;
            }
        }
        return null;
    }

    // ---------- 降级兜底数据 ----------

    private AiResultResponse<AiDiagnosisResponse> degradedDiagnosis() {
        return AiResultResponse.degraded(
                new AiDiagnosisResponse(List.of(), "AI 诊断服务不可用，请医生根据主诉、现病史及查体进行人工诊断"),
                "AI 诊断服务不可用，请医生根据主诉、现病史及查体进行人工诊断");
    }

    private AiResultResponse<AiExaminationResponse> degradedExamination() {
        // 兜底：提供常见检查项建议，避免完全无内容
        List<AiExaminationResponse.ExaminationItem> fallbackItems = List.of(
                new AiExaminationResponse.ExaminationItem("血常规", "检验", "通用基础检查"),
                new AiExaminationResponse.ExaminationItem("尿常规", "检验", "通用基础检查"),
                new AiExaminationResponse.ExaminationItem("心电图", "检查", "心血管基础筛查"),
                new AiExaminationResponse.ExaminationItem("胸部X光", "检查", "呼吸系统基础筛查")
        );
        return AiResultResponse.degraded(
                new AiExaminationResponse(fallbackItems),
                "AI 检查推荐服务不可用，已返回通用检查项建议，请医生结合临床判断");
    }

    private AiResultResponse<AiPrescriptionAssistResponse> degradedPrescriptionAssist() {
        return AiResultResponse.degraded(
                new AiPrescriptionAssistResponse(List.of(), "AI 辅助开方服务不可用，请医生根据诊疗规范与药品说明书手动开具处方"),
                "AI 辅助开方服务不可用，请医生根据诊疗规范与药品说明书手动开具处方");
    }

    private AiResultResponse<AiPrescriptionAuditResponse> degradedPrescriptionAudit() {
        return AiResultResponse.degraded(
                new AiPrescriptionAuditResponse(null,
                        List.of("AI 处方审核服务不可用，请药师进行人工审核，重点关注过敏史、配伍禁忌与用法用量"),
                        false),
                "AI 处方审核服务不可用，请药师进行人工审核，重点关注过敏史、配伍禁忌与用法用量");
    }

    private AiResultResponse<AiMedicalRecordGenResponse> degradedMedicalRecordGen() {
        return AiResultResponse.degraded(
                new AiMedicalRecordGenResponse("", "", "", "", ""),
                "AI 病历生成服务不可用，请医生使用病历模板手动录入病历内容");
    }
}
