package com.aimedical.modules.doctor.dto.response;

import java.util.List;

/**
 * AI 处方审核结果响应。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public record AiPrescriptionAuditResponse(
    String riskLevel,
    List<String> warnings,
    boolean passed
) {
}
