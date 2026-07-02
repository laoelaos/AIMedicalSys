package com.aimedical.modules.doctor.dto.response;

import java.time.LocalDateTime;

/**
 * 接诊/叫号队列响应 DTO。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public record ConsultationQueueResponse(
    Long id,
    Long patientId,
    String patientName,
    Long doctorId,
    String department,
    String queueNo,
    String status,
    LocalDateTime registeredAt,
    LocalDateTime calledAt,
    LocalDateTime finishedAt,
    String remark
) {
}
