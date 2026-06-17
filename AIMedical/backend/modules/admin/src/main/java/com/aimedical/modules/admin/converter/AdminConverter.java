package com.aimedical.modules.admin.converter;

import com.aimedical.modules.admin.dto.AdminDto;
import com.aimedical.modules.admin.entity.AdminEntity;

public interface AdminConverter {
    AdminDto toDto(AdminEntity entity);
    AdminEntity toEntity(AdminDto dto);
}
