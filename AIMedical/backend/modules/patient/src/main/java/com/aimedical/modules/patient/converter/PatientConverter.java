package com.aimedical.modules.patient.converter;

import com.aimedical.modules.patient.dto.PatientDto;
import com.aimedical.modules.patient.entity.PatientEntity;

public interface PatientConverter {
    PatientDto toDto(PatientEntity entity);
    PatientEntity toEntity(PatientDto dto);
}
