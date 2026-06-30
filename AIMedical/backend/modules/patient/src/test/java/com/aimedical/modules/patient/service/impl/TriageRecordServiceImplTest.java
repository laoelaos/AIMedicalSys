package com.aimedical.modules.patient.service.impl;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.modules.patient.dto.TriageRecordRequest;
import com.aimedical.modules.patient.dto.TriageRecordResponse;
import com.aimedical.modules.patient.entity.TriageRecordEntity;
import com.aimedical.modules.patient.repository.PatientTriageRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        when(repository.findByPatientIdAndDeletedFalseOrderByCreatedAtDesc(3L)).thenReturn(List.of(e));

        List<TriageRecordResponse> list = service.listByPatient(3L);
        assertEquals(1, list.size());
        assertEquals("头痛三天", list.get(0).getChiefComplaint());
        assertFalse(list.get(0).isDegraded());
    }

    @Test
    void listByTimeRangeShouldParseAndQuery() {
        TriageRecordEntity e = new TriageRecordEntity();
        e.setId(1L);
        e.setPatientId(3L);
        e.setChiefComplaint("头痛三天");
        e.setSessionId("sess-123");
        e.setIsDegraded(false);
        e.setCreatedAt(LocalDateTime.now());
        when(repository.findByPatientIdAndTimeRange(anyLong(), any(), any())).thenReturn(List.of(e));

        List<TriageRecordResponse> list = service.listByTimeRange(3L, "2026-06-01T00:00:00", "2026-06-30T23:59:59");
        assertEquals(1, list.size());
    }

    @Test
    void listByTimeRangeShouldThrowOnBadFormat() {
        assertThrows(BusinessException.class, () ->
                service.listByTimeRange(3L, "bad-format", "2026-06-30T23:59:59"));
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
        when(repository.findByPatientIdAndIsDegradedTrueAndDeletedFalseOrderByCreatedAtDesc(3L)).thenReturn(List.of(e));

        List<TriageRecordResponse> list = service.listDegraded(3L);
        assertEquals(1, list.size());
        assertTrue(list.get(0).isDegraded());
    }
}
