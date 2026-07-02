package com.aimedical.modules.doctor.converter;

import com.aimedical.modules.doctor.dto.response.ConsultationQueueResponse;
import com.aimedical.modules.doctor.entity.ConsultationQueueEntity;
import org.springframework.stereotype.Component;

/**
 * 接诊/叫号队列实体与 DTO 转换器。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Component
public class ConsultationQueueConverter {

    public ConsultationQueueResponse toResponse(ConsultationQueueEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ConsultationQueueResponse(
            entity.getId(),
            entity.getPatientId(),
            entity.getPatientName(),
            entity.getDoctorId(),
            entity.getDepartment(),
            entity.getQueueNo(),
            entity.getStatus(),
            entity.getRegisteredAt(),
            entity.getCalledAt(),
            entity.getFinishedAt(),
            entity.getRemark()
        );
    }
}
