package com.aimedical.modules.doctor.dto.response;

import java.math.BigDecimal;

/**
 * 处方明细 DTO（请求与响应共用）。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public record PrescriptionItemDto(
    Long id,
    String drugName,
    String specification,
    String dosage,
    String usageMethod,
    String frequency,
    BigDecimal quantity,
    String unit,
    String remark
) {
}
