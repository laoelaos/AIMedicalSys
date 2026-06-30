package com.aimedical.modules.doctor.service;

import com.aimedical.common.result.Result;

public interface DoctorService {
    Result<String> getPlaceholder();

    boolean existsById(Long id);

    String getRealName(Long id);
}
