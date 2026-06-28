package com.aimedical.modules.doctor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 处方明细创建请求。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public record PrescriptionItemRequest(
    @NotBlank @Size(max = 128) String drugName,
    @Size(max = 128) String specification,
    @Size(max = 64) String dosage,
    @Size(max = 128) String usageMethod,
    @Size(max = 64) String frequency,
    BigDecimal quantity,
    @Size(max = 32) String unit,
    @Size(max = 500) String remark
) {
}
