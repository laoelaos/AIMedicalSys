package com.aimedical.modules.doctor.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 处方响应 DTO。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public record PrescriptionResponse(
    Long id,
    Long patientId,
    String patientName,
    Long doctorId,
    String department,
    String status,
    String diagnosis,
    Boolean aiChecked,
    String aiRiskLevel,
    String auditRemark,
    Long auditedBy,
    LocalDateTime auditedAt,
    String remark,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<PrescriptionItemDto> items
) {
}
