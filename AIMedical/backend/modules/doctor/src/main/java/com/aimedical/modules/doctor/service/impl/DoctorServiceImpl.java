package com.aimedical.modules.doctor.service.impl;

import com.aimedical.common.result.Result;
import com.aimedical.modules.doctor.service.DoctorService;
import org.springframework.stereotype.Service;

@Service
public class DoctorServiceImpl implements DoctorService {

    @Override
    public Result<String> getPlaceholder() {
        return Result.success("doctor placeholder");
    }
}
