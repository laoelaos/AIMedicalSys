package com.aimedical.modules.doctor.service.impl;

import com.aimedical.common.result.Result;
import com.aimedical.modules.doctor.repository.DoctorRepository;
import com.aimedical.modules.doctor.service.DoctorService;
import org.springframework.stereotype.Service;

@Service
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;

    public DoctorServiceImpl(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    @Override
    public Result<String> getPlaceholder() {
        return Result.success("doctor placeholder");
    }

    @Override
    public boolean existsById(Long id) {
        return doctorRepository.existsById(id);
    }

    @Override
    public String getRealName(Long id) {
        return doctorRepository.findById(id)
                .map(doctor -> doctor.getRealName())
                .orElse(null);
    }
}
