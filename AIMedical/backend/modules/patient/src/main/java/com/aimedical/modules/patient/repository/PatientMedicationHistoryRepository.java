package com.aimedical.modules.patient.repository;

import com.aimedical.modules.patient.entity.PatientMedicationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientMedicationHistoryRepository extends JpaRepository<PatientMedicationHistory, Long> {

    List<PatientMedicationHistory> findByPatientId(Long patientId);
}
