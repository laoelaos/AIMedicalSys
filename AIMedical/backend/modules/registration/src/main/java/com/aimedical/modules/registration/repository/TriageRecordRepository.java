package com.aimedical.modules.registration.repository;

import com.aimedical.modules.registration.entity.TriageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TriageRecordRepository extends JpaRepository<TriageRecord, Long> {

    Optional<TriageRecord> findByRegistrationId(Long registrationId);

    List<TriageRecord> findByPatientId(Long patientId);
}