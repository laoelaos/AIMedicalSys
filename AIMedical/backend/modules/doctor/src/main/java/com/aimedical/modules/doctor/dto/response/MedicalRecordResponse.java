package com.aimedical.modules.doctor.dto.response;

import java.time.LocalDateTime;

/**
 * 病历响应 DTO。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public record MedicalRecordResponse(
    Long id,
    Long patientId,
    Long doctorId,
    String department,
    Integer versionNo,
    String status,
    String chiefComplaint,
    String presentIllness,
    String pastHistory,
    String diagnosis,
    String treatmentPlan,
    Long prescriptionId,
    Long templateId,
    Boolean aiGenerated,
    String remark,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
