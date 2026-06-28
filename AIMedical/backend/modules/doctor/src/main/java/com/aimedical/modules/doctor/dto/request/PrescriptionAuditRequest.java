package com.aimedical.modules.doctor.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 处方审核请求（通过/驳回）。
 *
 * <p>approve=true 表示审核通过；approve=false 表示驳回，需填写 auditRemark。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public record PrescriptionAuditRequest(
    @NotNull Boolean approve,
    @Size(max = 500) String auditRemark
) {
}
