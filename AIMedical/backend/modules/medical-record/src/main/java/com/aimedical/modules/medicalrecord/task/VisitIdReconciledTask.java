package com.aimedical.modules.medicalrecord.task;

import com.aimedical.modules.commonmodule.visit.VisitFacade;
import com.aimedical.modules.medicalrecord.entity.MedicalRecord;
import com.aimedical.modules.medicalrecord.repository.MedicalRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VisitIdReconciledTask {

    private static final Logger log = LoggerFactory.getLogger(VisitIdReconciledTask.class);

    private final VisitFacade visitFacade;
    private final MedicalRecordRepository medicalRecordRepository;

    public VisitIdReconciledTask(VisitFacade visitFacade, MedicalRecordRepository medicalRecordRepository) {
        this.visitFacade = visitFacade;
        this.medicalRecordRepository = medicalRecordRepository;
    }

    @Scheduled(cron = "0 */30 * * * ?")
    public void reconcileVisitIds() {
        List<MedicalRecord> records = medicalRecordRepository.findByVisitIdFallbackTrue();
        for (MedicalRecord record : records) {
            try {
                String encounterId = record.getVisitId();
                String visitId = visitFacade.findVisitIdByEncounterId(encounterId);
                if (visitId != null && !visitId.isBlank()) {
                    record.setVisitId(visitId);
                    record.setVisitIdFallback(false);
                    medicalRecordRepository.save(record);
                    log.info("Reconciled visitId for record {}: {}", record.getRecordId(), visitId);
                }
            } catch (Exception e) {
                log.warn("Failed to reconcile visitId for record {}: {}", record.getRecordId(), e.getMessage());
            }
        }
    }
}
