package com.aimedical.modules.doctor.dto.response;

import java.util.List;

/**
 * AI 辅助诊断响应。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public record AiDiagnosisResponse(
    List<String> possibleDiagnoses,
    String summary
) {
}
