package com.aimedical.modules.doctor.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * AI 病历生成请求。
 *
 * <p>根据主诉、现病史等结构化输入由 AI 生成完整病历文本；AI 不可用时降级为模板填充。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public record AiMedicalRecordGenRequest(
    @NotNull Long patientId,
    Long templateId,
    @Size(max = 500) String chiefComplaint,
    String presentIllness,
    String pastHistory,
    @Size(max = 500) String diagnosis
) {
}
