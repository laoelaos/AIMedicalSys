package com.aimedical.modules.consultation.repository;

import com.aimedical.modules.consultation.entity.TriageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface TriageRecordRepository extends JpaRepository<TriageRecord, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Transactional(propagation = Propagation.MANDATORY)
    Optional<TriageRecord> findBySessionId(String sessionId);

    Optional<TriageRecord> findTopByPatientIdOrderByTriageTimeDesc(String patientId);

    Optional<TriageRecord> findTopBySessionIdOrderByTriageTimeDesc(String sessionId);

    List<TriageRecord> findBySessionIdIn(List<String> sessionIds);
}
