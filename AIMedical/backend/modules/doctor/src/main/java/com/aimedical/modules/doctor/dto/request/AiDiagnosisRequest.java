package com.aimedical.modules.doctor.dto.request;

import jakarta.validation.constraints.Size;

/**
 * AI 辅助诊断请求（占位诊断入口）。
 *
 * <p>医生在病情录入后可请求 AI 给出初步诊断建议；AI 不可用时由兜底服务返回降级提示。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public record AiDiagnosisRequest(
    Long patientId,
    @Size(max = 500) String chiefComplaint,
    String presentIllness,
    String pastHistory
) {
}
