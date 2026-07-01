package com.aimedical.modules.prescription.repository;

import com.aimedical.modules.prescription.entity.AuditRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.List;
import java.util.Optional;

public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {

    List<AuditRecord> findByPrescriptionOrderIdOrderByAuditSequenceDesc(String prescriptionOrderId);

    Optional<AuditRecord> findTopByPrescriptionIdOrderByAuditSequenceDesc(String prescriptionId);

    Optional<AuditRecord> findTopByPrescriptionIdAndIsLatestTrueOrderByAuditSequenceDesc(String prescriptionId);

    Optional<AuditRecord> findTopByPrescriptionOrderIdAndIsLatestTrue(String prescriptionOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<AuditRecord> findByPrescriptionIdAndIsLatestTrue(String prescriptionId);

    List<AuditRecord> findByPrescriptionOrderIdAndIsLatestTrue(String prescriptionOrderId);
}
