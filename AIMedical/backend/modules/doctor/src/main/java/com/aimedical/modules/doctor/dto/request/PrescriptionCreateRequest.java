package com.aimedical.modules.doctor.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 创建处方请求。
 *
 * <p>处方创建后默认为 DRAFT 草稿状态；若 submitForReview=true 则直接流转到 PENDING_REVIEW。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public record PrescriptionCreateRequest(
    @NotNull Long patientId,
    @Size(max = 500) String diagnosis,
    @Size(max = 500) String remark,
    boolean submitForReview,
    @Valid @NotEmpty List<PrescriptionItemRequest> items
) {
}
