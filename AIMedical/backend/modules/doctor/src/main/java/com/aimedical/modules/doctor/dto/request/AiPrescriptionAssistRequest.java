package com.aimedical.modules.doctor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * AI 辅助开方请求。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public record AiPrescriptionAssistRequest(
    Long patientId,
    @NotBlank @Size(max = 500) String diagnosis,
    @Size(max = 500) String chiefComplaint
) {
}
