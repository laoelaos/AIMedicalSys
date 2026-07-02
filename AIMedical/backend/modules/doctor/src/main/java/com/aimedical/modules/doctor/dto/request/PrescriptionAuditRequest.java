package com.aimedical.modules.doctor.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 处方审核请求（通过/驳回）。
 *
 * <p>approve=true 表示审核通过；approve=false 表示驳回，需填写 auditRemark。
 *
 * <p>校验分层：
 * <ul>
 *   <li>Bean Validation：@AssertTrue 保证 approve=false 时 auditRemark 非空非空白</li>
 *   <li>Service 层：手动校验作为防御性兜底（见 PrescriptionServiceImpl.audit）</li>
 * </ul>
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public record PrescriptionAuditRequest(
    @NotNull Boolean approve,
    @Size(max = 500) String auditRemark
) {

    /**
     * 驳回时（approve=false）必须填写驳回原因，approve=true 时不强制。
     */
    @AssertTrue(message = "驳回时必须填写驳回原因")
    public boolean isRemarkValid() {
        return approve == null || approve || (auditRemark != null && !auditRemark.isBlank());
    }
}
