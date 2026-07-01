package com.aimedical.modules.medicalrecord.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import com.aimedical.modules.medicalrecord.entity.MedicalRecord;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    Optional<MedicalRecord> findByVisitId(String visitId);
    Optional<MedicalRecord> findByPatientId(String patientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<MedicalRecord> findByVisitIdFallbackTrue();
}
