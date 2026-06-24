package com.aimedical.modules.patient.repository;

import com.aimedical.modules.patient.entity.PatientChronicDisease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientChronicDiseaseRepository extends JpaRepository<PatientChronicDisease, Long> {

    List<PatientChronicDisease> findByPatientId(Long patientId);
}
