package com.aimedical.modules.patient.repository;

import com.aimedical.modules.patient.entity.PatientFamilyHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientFamilyHistoryRepository extends JpaRepository<PatientFamilyHistory, Long> {

    List<PatientFamilyHistory> findByPatientId(Long patientId);
}
