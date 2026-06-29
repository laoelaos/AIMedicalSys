package com.aimedical.modules.patient.repository;

import com.aimedical.modules.patient.entity.TriageRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TriageRecordRepository extends JpaRepository<TriageRecordEntity, Long> {

    List<TriageRecordEntity> findByPatientIdAndDeletedFalseOrderByCreatedAtDesc(Long patientId);

    @Query("SELECT t FROM TriageRecordEntity t WHERE t.patientId = :patientId AND t.deleted = false " +
           "AND t.createdAt >= :startTime AND t.createdAt <= :endTime ORDER BY t.createdAt DESC")
    List<TriageRecordEntity> findByPatientIdAndTimeRange(@Param("patientId") Long patientId,
                                                           @Param("startTime") LocalDateTime startTime,
                                                           @Param("endTime") LocalDateTime endTime);

    List<TriageRecordEntity> findByPatientIdAndIsDegradedTrueAndDeletedFalseOrderByCreatedAtDesc(Long patientId);
}
