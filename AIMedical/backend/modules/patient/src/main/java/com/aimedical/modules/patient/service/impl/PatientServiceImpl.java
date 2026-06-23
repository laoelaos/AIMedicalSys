package com.aimedical.modules.patient.service.impl;

import com.aimedical.common.result.Result;
import com.aimedical.modules.patient.service.PatientService;
import org.springframework.stereotype.Service;

@Service
public class PatientServiceImpl implements PatientService {

    @Override
    public Result<String> getPlaceholder() {
        return Result.success("patient placeholder");
    }
}
