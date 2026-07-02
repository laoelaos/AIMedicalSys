package com.aimedical.modules.doctor.dto.response;

/**
 * AI 病历生成结果响应。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public record AiMedicalRecordGenResponse(
    String chiefComplaint,
    String presentIllness,
    String pastHistory,
    String diagnosis,
    String treatmentPlan
) {
}
