package com.aimedical.modules.doctor.dto.response;

/**
 * 病历模板响应 DTO。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public record MedicalRecordTemplateResponse(
    Long id,
    String department,
    String name,
    String chiefComplaintTpl,
    String presentIllnessTpl,
    String pastHistoryTpl,
    String diagnosisTpl,
    String treatmentPlanTpl,
    Boolean enabled,
    String remark
) {
}
