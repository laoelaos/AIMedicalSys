package com.aimedical.modules.doctor.dto.response;

import java.util.List;

/**
 * AI 辅助开方响应。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public record AiPrescriptionAssistResponse(
    List<RecommendedDrug> drugs,
    String summary
) {

    /**
     * 推荐药品。
     */
    public record RecommendedDrug(
        String drugName,
        String specification,
        String dosage,
        String frequency,
        String reason
    ) {
    }
}
