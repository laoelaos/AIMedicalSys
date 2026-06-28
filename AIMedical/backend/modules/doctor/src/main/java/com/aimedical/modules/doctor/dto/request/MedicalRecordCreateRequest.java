package com.aimedical.modules.doctor.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建病历请求。
 *
 * <p>支持版本管理：若 patientId 已存在草稿则更新该草稿，否则新建草稿；
 * 若 publish=true，则将当前草稿转为正式(OFFICIAL)，并自增 version_no。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public record MedicalRecordCreateRequest(
    @NotNull Long patientId,
    Long templateId,
    Long prescriptionId,
    @Size(max = 500) String chiefComplaint,
    String presentIllness,
    String pastHistory,
    String diagnosis,
    String treatmentPlan,
    @Size(max = 500) String remark,
    boolean publish
) {
}
