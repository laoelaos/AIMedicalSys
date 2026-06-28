package com.aimedical.modules.patient.service;

import com.aimedical.common.result.Result;

public interface PatientService {
    Result<String> getPlaceholder();

    boolean existsById(Long id);

    String getRealName(Long id);
}
