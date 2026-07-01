package com.aimedical.modules.patient.service.impl;

import com.aimedical.modules.patient.dto.TriageRecordRequest;
import com.aimedical.modules.patient.dto.TriageRecordResponse;
import com.aimedical.modules.patient.entity.TriageRecordEntity;
import com.aimedical.modules.patient.repository.PatientTriageRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TriageRecordServiceImplTest {

    @Mock private PatientTriageRecordRepository repository;
    @InjectMocks private TriageRecordServiceImpl service;

    @Test
    void saveAsyncShouldPersistRecord() {
        TriageRecordRequest req = new TriageRecordRequest();
        req.setPatientId(3L);
        req.setChiefComplaint("头痛三天");
        req.setSessionId("sess-123");
        req.setDegraded(false);
        req.setRuleVersion("v1.0.0");
        req.setRuleSetId("rule-set-1");
        req.setRecommendedDepartments(List.of("神经内科", "普通内科"));
        req.setRecommendedDoctors(List.of("王主任"));
        req.setMatchedRules(List.of("头痛规则-偏头痛"));

        service.saveAsync(req);
        verify(repository, timeout(2000)).save(any(TriageRecordEntity.class));
    }

    @Test
    void listByPatientShouldReturnRecords() {
        TriageRecordEntity e = createEntity();
        Page<TriageRecordEntity> page = new PageImpl<>(List.of(e));
        when(repository.findByPatientIdAndDeletedFalseOrderByCreatedAtDesc(eq(3L), any(Pageable.class)))
                .thenReturn(page);

        Page<TriageRecordResponse> result = service.listByPatient(3L, Pageable.unpaged());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void listByTimeRangeShouldParseAndQuery() {
        TriageRecordEntity e = createEntity();
        Page<TriageRecordEntity> page = new PageImpl<>(List.of(e));
        when(repository.findByPatientIdAndTimeRange(anyLong(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        Page<TriageRecordResponse> result = service.listByTimeRange(3L,
                LocalDateTime.of(2026, 6, 1, 0, 0),
                LocalDateTime.of(2026, 6, 30, 23, 59),
                Pageable.unpaged());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void listDegradedShouldReturnDegradedOnly() {
        TriageRecordEntity e = new TriageRecordEntity();
        e.setId(1L);
        e.setPatientId(3L);
        e.setChiefComplaint("腹痛1天");
        e.setSessionId("sess-degraded");
        e.setIsDegraded(true);
        e.setCreatedAt(LocalDateTime.now());
        Page<TriageRecordEntity> page = new PageImpl<>(List.of(e));
        when(repository.findByPatientIdAndIsDegradedTrueAndDeletedFalseOrderByCreatedAtDesc(eq(3L), any(Pageable.class)))
                .thenReturn(page);

        Page<TriageRecordResponse> result = service.listDegraded(3L, Pageable.unpaged());
        assertEquals(1, result.getTotalElements());
    }

    private TriageRecordEntity createEntity() {
        TriageRecordEntity e = new TriageRecordEntity();
        e.setId(1L);
        e.setPatientId(3L);
        e.setChiefComplaint("头痛三天");
        e.setSessionId("sess-123");
        e.setIsDegraded(false);
        e.setRuleVersion("v1.0.0");
        e.setRuleSetId("rule-set-1");
        e.setRecommendedDepartments("神经内科,普通内科");
        e.setRecommendedDoctors("王主任");
        e.setMatchedRules("头痛规则-偏头痛");
        e.setCreatedAt(LocalDateTime.now());
        return e;
    }
}
