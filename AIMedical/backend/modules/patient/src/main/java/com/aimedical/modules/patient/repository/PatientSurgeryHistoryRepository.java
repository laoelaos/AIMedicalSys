package com.aimedical.modules.patient.repository;

import com.aimedical.modules.patient.entity.PatientSurgeryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientSurgeryHistoryRepository extends JpaRepository<PatientSurgeryHistory, Long> {

    List<PatientSurgeryHistory> findByPatientId(Long patientId);
}
