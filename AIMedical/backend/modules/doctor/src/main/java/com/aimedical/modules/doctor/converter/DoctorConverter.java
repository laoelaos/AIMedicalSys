package com.aimedical.modules.doctor.converter;

import com.aimedical.modules.doctor.dto.DoctorDto;
import com.aimedical.modules.doctor.entity.DoctorEntity;

public interface DoctorConverter {
    DoctorDto toDto(DoctorEntity entity);
    DoctorEntity toEntity(DoctorDto dto);
}
