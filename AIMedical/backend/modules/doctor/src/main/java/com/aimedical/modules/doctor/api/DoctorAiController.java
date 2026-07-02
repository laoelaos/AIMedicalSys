package com.aimedical.modules.doctor.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.auth.CurrentUser;
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
import com.aimedical.modules.doctor.service.DoctorAiService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 医生端 AI 控制器。
 *
 * <p>提供占位诊断、开立检查、辅助开方、处方审核、病历生成 5 个 AI 入口。
 * 所有接口返回 {@link AiResultResponse}，AI 不可用时显式返回降级结果与兜底数据，
 * 前端据此展示降级标识 UI。全部需要 DOCTOR 角色。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/doctor/ai")
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorAiController {

    private final DoctorAiService doctorAiService;
    private final CurrentUser currentUser;

    public DoctorAiController(DoctorAiService doctorAiService, CurrentUser currentUser) {
        this.doctorAiService = doctorAiService;
        this.currentUser = currentUser;
    }

    /**
     * 占位诊断入口。
     */
    @PostMapping("/diagnosis")
    public Result<AiResultResponse<AiDiagnosisResponse>> diagnosis(@Valid @RequestBody AiDiagnosisRequest request) {
        return doctorAiService.diagnosis(request, currentDoctorId());
    }

    /**
     * 开立检查推荐入口。
     */
    @PostMapping("/examination")
    public Result<AiResultResponse<AiExaminationResponse>> recommendExamination(@Valid @RequestBody AiExaminationRequest request) {
        return doctorAiService.recommendExamination(request, currentDoctorId());
    }

    /**
     * 辅助开方入口。
     */
    @PostMapping("/prescription-assist")
    public Result<AiResultResponse<AiPrescriptionAssistResponse>> prescriptionAssist(@Valid @RequestBody AiPrescriptionAssistRequest request) {
        return doctorAiService.prescriptionAssist(request, currentDoctorId());
    }

    /**
     * 处方审核入口。
     */
    @PostMapping("/prescription-audit")
    public Result<AiResultResponse<AiPrescriptionAuditResponse>> prescriptionAudit(@Valid @RequestBody AiPrescriptionAuditRequest request) {
        return doctorAiService.prescriptionAudit(request, currentDoctorId());
    }

    /**
     * 病历生成入口。
     */
    @PostMapping("/medical-record-gen")
    public Result<AiResultResponse<AiMedicalRecordGenResponse>> generateMedicalRecord(@Valid @RequestBody AiMedicalRecordGenRequest request) {
        return doctorAiService.generateMedicalRecord(request, currentDoctorId());
    }

    private Long currentDoctorId() {
        Long userId = currentUser.getUserId();
        if (userId == null) {
            throw new IllegalStateException("无法获取当前登录医生ID");
        }
        return userId;
    }
}
