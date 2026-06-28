package com.aimedical.modules.doctor.dto.request;

import jakarta.validation.constraints.Size;

/**
 * AI 开立检查推荐请求。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public record AiExaminationRequest(
    Long patientId,
    @Size(max = 500) String diagnosis,
    @Size(max = 500) String chiefComplaint
) {
}
