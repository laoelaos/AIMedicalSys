package com.aimedical.modules.patient.repository;

import com.aimedical.modules.patient.entity.PatientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<PatientEntity, Long> {
}
