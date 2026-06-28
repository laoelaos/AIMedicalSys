package com.aimedical.modules.doctor.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * AI 处方审核请求。
 *
 * <p>传入待审核处方 ID 与明细，由 AI 给出风险等级与提示；AI 不可用时降级为人工审核。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public record AiPrescriptionAuditRequest(
    @NotNull Long prescriptionId,
    @Size(max = 500) String diagnosis,
    @NotNull List<@Size(max = 128) String> drugNames
) {
}
