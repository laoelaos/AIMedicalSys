package com.aimedical.modules.patient.adapter;

import com.aimedical.modules.commonmodule.patient.PatientInfoPort;
import com.aimedical.modules.patient.entity.PatientEntity;
import com.aimedical.modules.patient.repository.PatientRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * {@link PatientInfoPort} 在 patient 模块的实现。
 *
 * <p>将 patient 模块的 PatientRepository 封装为端口实现，
 * 供 doctor 等消费方通过接口注入，消除编译期跨模块硬依赖。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Component
public class PatientInfoAdapter implements PatientInfoPort {

    private final PatientRepository patientRepository;

    public PatientInfoAdapter(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public Optional<String> findNameById(Long patientId) {
        return patientRepository.findById(patientId)
                .map(PatientEntity::getRealName);
    }
}
