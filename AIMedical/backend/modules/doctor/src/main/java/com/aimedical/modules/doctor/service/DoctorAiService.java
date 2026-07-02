package com.aimedical.modules.doctor.service;

import com.aimedical.common.result.Result;
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

/**
 * 医生端 AI 服务（带降级包装）。
 *
 * <p>所有方法返回 {@link AiResultResponse}，当 AI 不可用时显式返回降级结果与兜底数据，
 * 前端据此展示降级标识 UI。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public interface DoctorAiService {

    Result<AiResultResponse<AiDiagnosisResponse>> diagnosis(AiDiagnosisRequest request, Long doctorUserId);

    Result<AiResultResponse<AiExaminationResponse>> recommendExamination(AiExaminationRequest request, Long doctorUserId);

    Result<AiResultResponse<AiPrescriptionAssistResponse>> prescriptionAssist(AiPrescriptionAssistRequest request, Long doctorUserId);

    Result<AiResultResponse<AiPrescriptionAuditResponse>> prescriptionAudit(AiPrescriptionAuditRequest request, Long doctorUserId);

    Result<AiResultResponse<AiMedicalRecordGenResponse>> generateMedicalRecord(AiMedicalRecordGenRequest request, Long doctorUserId);
}
