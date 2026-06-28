package com.aimedical.modules.doctor.dto.response;

import java.util.List;

/**
 * AI 开立检查推荐响应。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public record AiExaminationResponse(
    List<ExaminationItem> items
) {

    /**
     * 推荐检查项。
     */
    public record ExaminationItem(
        String name,
        String category,
        String reason
    ) {
    }
}
