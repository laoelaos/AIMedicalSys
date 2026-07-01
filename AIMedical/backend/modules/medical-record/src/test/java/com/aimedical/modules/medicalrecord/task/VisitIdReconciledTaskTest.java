package com.aimedical.modules.medicalrecord.task;

import com.aimedical.modules.commonmodule.visit.VisitFacade;
import com.aimedical.modules.medicalrecord.entity.MedicalRecord;
import com.aimedical.modules.medicalrecord.repository.MedicalRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class VisitIdReconciledTaskTest {

    private StubVisitFacade visitFacade;
    private StubMedicalRecordRepository recordRepository;
    private VisitIdReconciledTask task;

    @BeforeEach
    void setUp() {
        visitFacade = new StubVisitFacade();
        recordRepository = new StubMedicalRecordRepository();
        task = new VisitIdReconciledTask(visitFacade, recordRepository);
    }

    @Test
    void shouldOnlyScanFallbackTrueRecords() {
        MedicalRecord fallbackRecord = new MedicalRecord();
        fallbackRecord.setRecordId(1L);
        fallbackRecord.setPatientId("P001");
        fallbackRecord.setVisitId("ENC-001");
        fallbackRecord.setVisitIdFallback(true);
        recordRepository.fallbackRecords.add(fallbackRecord);
        visitFacade.visitId = "VISIT-001";

        task.reconcileVisitIds();

        assertEquals("VISIT-001", fallbackRecord.getVisitId());
        assertFalse(fallbackRecord.getVisitIdFallback());
        assertTrue(recordRepository.saved);
    }

    @Test
    void shouldUseGetVisitIdAsEncounterIdForLookup() {
        MedicalRecord record = new MedicalRecord();
        record.setRecordId(1L);
        record.setPatientId("P001");
        record.setVisitId("ENC-XYZ");
        record.setVisitIdFallback(true);
        recordRepository.fallbackRecords.add(record);
        visitFacade.encounterIdToVisitId.put("ENC-XYZ", "VISIT-RESOLVED");

        task.reconcileVisitIds();

        assertEquals("VISIT-RESOLVED", record.getVisitId());
        assertFalse(record.getVisitIdFallback());
        assertEquals("ENC-XYZ", visitFacade.lastQueriedEncounterId);
    }

    @Test
    void shouldResetVisitIdFallbackOnSuccessfulReconcile() {
        MedicalRecord record = new MedicalRecord();
        record.setRecordId(1L);
        record.setPatientId("P001");
        record.setVisitId("ENC-001");
        record.setVisitIdFallback(true);
        recordRepository.fallbackRecords.add(record);
        visitFacade.visitId = "VISIT-001";

        task.reconcileVisitIds();

        assertFalse(record.getVisitIdFallback());
    }

    @Test
    void shouldKeepFallbackTrueWhenFacadeReturnsNull() {
        MedicalRecord record = new MedicalRecord();
        record.setRecordId(1L);
        record.setPatientId("P001");
        record.setVisitId("ENC-001");
        record.setVisitIdFallback(true);
        recordRepository.fallbackRecords.add(record);
        visitFacade.visitId = null;

        task.reconcileVisitIds();

        assertEquals("ENC-001", record.getVisitId());
        assertTrue(record.getVisitIdFallback());
        assertFalse(recordRepository.saved);
    }

    @Test
    void shouldKeepFallbackTrueWhenFacadeReturnsBlank() {
        MedicalRecord record = new MedicalRecord();
        record.setRecordId(1L);
        record.setPatientId("P001");
        record.setVisitId("ENC-001");
        record.setVisitIdFallback(true);
        recordRepository.fallbackRecords.add(record);
        visitFacade.visitId = "   ";

        task.reconcileVisitIds();

        assertEquals("ENC-001", record.getVisitId());
        assertTrue(record.getVisitIdFallback());
        assertFalse(recordRepository.saved);
    }

    @Test
    void shouldKeepFallbackTrueWhenFacadeReturnsEmpty() {
        MedicalRecord record = new MedicalRecord();
        record.setRecordId(1L);
        record.setPatientId("P001");
        record.setVisitId("ENC-001");
        record.setVisitIdFallback(true);
        recordRepository.fallbackRecords.add(record);
        visitFacade.visitId = "";

        task.reconcileVisitIds();

        assertEquals("ENC-001", record.getVisitId());
        assertTrue(record.getVisitIdFallback());
        assertFalse(recordRepository.saved);
    }

    @Test
    void shouldContinueOnExceptionAndKeepFallbackTrueForFailedRecord() {
        MedicalRecord record1 = new MedicalRecord();
        record1.setRecordId(1L);
        record1.setPatientId("P001");
        record1.setVisitId("ENC-FAIL");
        record1.setVisitIdFallback(true);
        recordRepository.fallbackRecords.add(record1);

        MedicalRecord record2 = new MedicalRecord();
        record2.setRecordId(2L);
        record2.setPatientId("P002");
        record2.setVisitId("ENC-OK");
        record2.setVisitIdFallback(true);
        recordRepository.fallbackRecords.add(record2);

        visitFacade.throwOnEncounterId = "ENC-FAIL";
        visitFacade.visitId = "VISIT-OK";

        task.reconcileVisitIds();

        assertEquals("ENC-FAIL", record1.getVisitId());
        assertTrue(record1.getVisitIdFallback());
        assertEquals("VISIT-OK", record2.getVisitId());
        assertFalse(record2.getVisitIdFallback());
    }

    @Test
    void shouldNotScanRecordsWithFallbackFalse() {
        MedicalRecord nonFallbackRecord = new MedicalRecord();
        nonFallbackRecord.setRecordId(1L);
        nonFallbackRecord.setPatientId("P001");
        nonFallbackRecord.setVisitId("VISIT-EXISTING");
        nonFallbackRecord.setVisitIdFallback(false);
        recordRepository.fallbackRecords.add(nonFallbackRecord);

        task.reconcileVisitIds();

        assertEquals("VISIT-EXISTING", nonFallbackRecord.getVisitId());
        assertFalse(nonFallbackRecord.getVisitIdFallback());
        assertFalse(recordRepository.saved);
    }

    @Test
    void shouldHandleEmptyFallbackList() {
        task.reconcileVisitIds();

        assertFalse(recordRepository.saved);
    }

    @Test
    void shouldReconcileMultipleFallbackRecords() {
        MedicalRecord record1 = new MedicalRecord();
        record1.setRecordId(1L);
        record1.setPatientId("P001");
        record1.setVisitId("ENC-001");
        record1.setVisitIdFallback(true);
        recordRepository.fallbackRecords.add(record1);

        MedicalRecord record2 = new MedicalRecord();
        record2.setRecordId(2L);
        record2.setPatientId("P002");
        record2.setVisitId("ENC-002");
        record2.setVisitIdFallback(true);
        recordRepository.fallbackRecords.add(record2);

        visitFacade.encounterIdToVisitId.put("ENC-001", "VISIT-001");
        visitFacade.encounterIdToVisitId.put("ENC-002", "VISIT-002");

        task.reconcileVisitIds();

        assertEquals("VISIT-001", record1.getVisitId());
        assertFalse(record1.getVisitIdFallback());
        assertEquals("VISIT-002", record2.getVisitId());
        assertFalse(record2.getVisitIdFallback());
    }

    @Test
    void shouldSaveRecordAfterSuccessfulReconcile() {
        MedicalRecord record = new MedicalRecord();
        record.setRecordId(1L);
        record.setPatientId("P001");
        record.setVisitId("ENC-001");
        record.setVisitIdFallback(true);
        recordRepository.fallbackRecords.add(record);
        visitFacade.visitId = "VISIT-001";

        task.reconcileVisitIds();

        assertTrue(recordRepository.saved);
        assertEquals(record, recordRepository.lastSavedRecord);
    }

    private static class StubVisitFacade implements VisitFacade {
        String visitId;
        String throwOnEncounterId;
        String lastQueriedEncounterId;
        java.util.Map<String, String> encounterIdToVisitId = new java.util.HashMap<>();

        @Override
        public String findVisitIdByEncounterId(String encounterId) {
            lastQueriedEncounterId = encounterId;
            if (throwOnEncounterId != null && throwOnEncounterId.equals(encounterId)) {
                throw new RuntimeException("VisitFacade error for " + encounterId);
            }
            if (!encounterIdToVisitId.isEmpty()) {
                return encounterIdToVisitId.get(encounterId);
            }
            return visitId;
        }
    }

    private static class StubMedicalRecordRepository implements MedicalRecordRepository {
        final List<MedicalRecord> fallbackRecords = new ArrayList<>();
        boolean saved = false;
        MedicalRecord lastSavedRecord = null;

        @Override
        public List<MedicalRecord> findByVisitIdFallbackTrue() {
            return fallbackRecords.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getVisitIdFallback()))
                    .toList();
        }

        @Override
        public List<MedicalRecord> findAll() { return fallbackRecords; }

        @Override
        public <S extends MedicalRecord> S save(S entity) {
            saved = true;
            lastSavedRecord = entity;
            return entity;
        }

        @Override
        public Optional<MedicalRecord> findByVisitId(String visitId) { return Optional.empty(); }
        @Override
        public Optional<MedicalRecord> findByPatientId(String patientId) { return Optional.empty(); }
        @Override
        public Optional<MedicalRecord> findById(Long id) { return Optional.empty(); }
        @Override
        public boolean existsById(Long id) { return false; }
        @Override
        public List<MedicalRecord> findAllById(Iterable<Long> ids) { return Collections.emptyList(); }
        @Override
        public long count() { return 0; }
        @Override
        public void deleteById(Long id) {}
        @Override
        public void delete(MedicalRecord entity) {}
        @Override
        public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override
        public void deleteAll(Iterable<? extends MedicalRecord> entities) {}
        @Override
        public void deleteAll() {}
        @Override
        public <S extends MedicalRecord> List<S> saveAll(Iterable<S> entities) { return null; }
        @Override
        public void flush() {}
        @Override
        public <S extends MedicalRecord> S saveAndFlush(S entity) { return entity; }
        @Override
        public <S extends MedicalRecord> List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override
        public void deleteAllInBatch(Iterable<MedicalRecord> entities) {}
        @Override
        public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override
        public void deleteAllInBatch() {}
        @Override
        public MedicalRecord getOne(Long id) { return null; }
        @Override
        public MedicalRecord getById(Long id) { return null; }
        @Override
        public MedicalRecord getReferenceById(Long id) { return null; }
        @Override
        public <S extends MedicalRecord> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override
        public <S extends MedicalRecord> List<S> findAll(Example<S> example) { return null; }
        @Override
        public <S extends MedicalRecord> List<S> findAll(Example<S> example, Sort sort) { return null; }
        @Override
        public <S extends MedicalRecord> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override
        public <S extends MedicalRecord> long count(Example<S> example) { return 0; }
        @Override
        public <S extends MedicalRecord> boolean exists(Example<S> example) { return false; }
        @Override
        public <S extends MedicalRecord, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override
        public List<MedicalRecord> findAll(Sort sort) { return Collections.emptyList(); }
        @Override
        public Page<MedicalRecord> findAll(Pageable pageable) { return null; }
    }
}
