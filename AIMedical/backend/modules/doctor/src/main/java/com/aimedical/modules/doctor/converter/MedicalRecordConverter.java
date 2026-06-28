package com.aimedical.modules.doctor.converter;

import com.aimedical.modules.doctor.dto.response.MedicalRecordResponse;
import com.aimedical.modules.doctor.dto.response.MedicalRecordTemplateResponse;
import com.aimedical.modules.doctor.entity.MedicalRecordEntity;
import com.aimedical.modules.doctor.entity.MedicalRecordTemplateEntity;
import org.springframework.stereotype.Component;

/**
 * 病历实体与 DTO 转换器。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Component
public class MedicalRecordConverter {

    public MedicalRecordResponse toResponse(MedicalRecordEntity entity) {
        if (entity == null) {
            return null;
        }
        return new MedicalRecordResponse(
            entity.getId(),
            entity.getPatientId(),
            entity.getDoctorId(),
            entity.getDepartment(),
            entity.getVersionNo(),
            entity.getStatus(),
            entity.getChiefComplaint(),
            entity.getPresentIllness(),
            entity.getPastHistory(),
            entity.getDiagnosis(),
            entity.getTreatmentPlan(),
            entity.getPrescriptionId(),
            entity.getTemplateId(),
            entity.getAiGenerated(),
            entity.getRemark(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public MedicalRecordTemplateResponse toTemplateResponse(MedicalRecordTemplateEntity entity) {
        if (entity == null) {
            return null;
        }
        return new MedicalRecordTemplateResponse(
            entity.getId(),
            entity.getDepartment(),
            entity.getName(),
            entity.getChiefComplaintTpl(),
            entity.getPresentIllnessTpl(),
            entity.getPastHistoryTpl(),
            entity.getDiagnosisTpl(),
            entity.getTreatmentPlanTpl(),
            entity.getEnabled(),
            entity.getRemark()
        );
    }
}
