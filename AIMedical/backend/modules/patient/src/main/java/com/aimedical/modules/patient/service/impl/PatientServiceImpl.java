package com.aimedical.modules.patient.service.impl;

import com.aimedical.common.result.Result;
import com.aimedical.modules.patient.repository.PatientRepository;
import com.aimedical.modules.patient.service.PatientService;
import org.springframework.stereotype.Service;

@Service
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;

    public PatientServiceImpl(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public Result<String> getPlaceholder() {
        return Result.success("patient placeholder");
    }

    @Override
    public boolean existsById(Long id) {
        return patientRepository.existsById(id);
    }

    @Override
    public String getRealName(Long id) {
        return patientRepository.findById(id)
                .map(patient -> patient.getRealName())
                .orElse(null);
    }
}
