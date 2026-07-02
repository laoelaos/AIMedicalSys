package com.aimedical.modules.doctor.converter;

import com.aimedical.modules.doctor.dto.request.PrescriptionItemRequest;
import com.aimedical.modules.doctor.dto.response.PrescriptionItemDto;
import com.aimedical.modules.doctor.dto.response.PrescriptionResponse;
import com.aimedical.modules.doctor.entity.PrescriptionEntity;
import com.aimedical.modules.doctor.entity.PrescriptionItemEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 处方实体与 DTO 转换器。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Component
public class PrescriptionConverter {

    public PrescriptionResponse toResponse(PrescriptionEntity entity) {
        if (entity == null) {
            return null;
        }
        List<PrescriptionItemDto> itemDtos = Optional.ofNullable(entity.getItems())
                .orElse(Collections.emptyList())
                .stream()
                .map(this::toItemDto)
                .toList();
        return new PrescriptionResponse(
            entity.getId(),
            entity.getPatientId(),
            entity.getPatientName(),
            entity.getDoctorId(),
            entity.getDepartment(),
            entity.getStatus(),
            entity.getDiagnosis(),
            entity.getAiChecked(),
            entity.getAiRiskLevel(),
            entity.getAuditRemark(),
            entity.getAuditedBy(),
            entity.getAuditedAt(),
            entity.getRemark(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            itemDtos
        );
    }

    public PrescriptionItemDto toItemDto(PrescriptionItemEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PrescriptionItemDto(
            entity.getId(),
            entity.getDrugName(),
            entity.getSpecification(),
            entity.getDosage(),
            entity.getUsageMethod(),
            entity.getFrequency(),
            entity.getQuantity(),
            entity.getUnit(),
            entity.getRemark()
        );
    }

    public PrescriptionItemEntity toItemEntity(PrescriptionItemRequest request) {
        if (request == null) {
            return null;
        }
        PrescriptionItemEntity entity = new PrescriptionItemEntity();
        entity.setDrugName(request.drugName());
        entity.setSpecification(request.specification());
        entity.setDosage(request.dosage());
        entity.setUsageMethod(request.usageMethod());
        entity.setFrequency(request.frequency());
        entity.setQuantity(request.quantity());
        entity.setUnit(request.unit());
        entity.setRemark(request.remark());
        return entity;
    }
}
