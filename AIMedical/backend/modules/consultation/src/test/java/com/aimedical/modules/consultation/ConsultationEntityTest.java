package com.aimedical.modules.consultation;

import com.aimedical.modules.consultation.entity.DeadLetterEvent;
import com.aimedical.modules.consultation.entity.TriageRecord;
import com.aimedical.modules.consultation.rule.entity.TriageRule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ConsultationEntityTest {

    @Test
    void shouldCreateTriageRecordWithDefaultConstructor() {
        TriageRecord record = new TriageRecord();
        assertNull(record.getSessionId());
        assertNull(record.getPatientId());
        assertNull(record.getChiefComplaint());
        assertNull(record.getAiRecommendedDepartments());
        assertNull(record.getRecommendedDoctors());
        assertNull(record.getRuleMatchedDepartments());
        assertNull(record.getFinalDepartmentId());
        assertNull(record.getFinalDepartmentName());
        assertNull(record.getConfidence());
        assertNull(record.getDegraded());
        assertNull(record.getRuleVersion());
        assertNull(record.getRuleSetId());
        assertNull(record.getTriageTime());
    }

    @Test
    void shouldSetAndGetTriageRecordFields() {
        TriageRecord record = new TriageRecord();
        record.setSessionId("session-001");
        record.setPatientId("P001");
        record.setChiefComplaint("头痛三天");
        record.setAiRecommendedDepartments("[{\"departmentId\":\"dept-01\"}]");
        record.setRecommendedDoctors("[{\"doctorId\":\"D001\"}]");
        record.setRuleMatchedDepartments("[{\"departmentId\":\"dept-02\"}]");
        record.setFinalDepartmentId("dept-01");
        record.setFinalDepartmentName("神经内科");
        record.setConfidence(0.85f);
        record.setDegraded(false);
        record.setRuleVersion("v1.0");
        record.setRuleSetId("RS001");
        LocalDateTime now = LocalDateTime.now();
        record.setTriageTime(now);

        assertEquals("session-001", record.getSessionId());
        assertEquals("P001", record.getPatientId());
        assertEquals("头痛三天", record.getChiefComplaint());
        assertEquals("[{\"departmentId\":\"dept-01\"}]", record.getAiRecommendedDepartments());
        assertEquals("[{\"doctorId\":\"D001\"}]", record.getRecommendedDoctors());
        assertEquals("[{\"departmentId\":\"dept-02\"}]", record.getRuleMatchedDepartments());
        assertEquals("dept-01", record.getFinalDepartmentId());
        assertEquals("神经内科", record.getFinalDepartmentName());
        assertEquals(0.85f, record.getConfidence(), 0.001f);
        assertFalse(record.getDegraded());
        assertEquals("v1.0", record.getRuleVersion());
        assertEquals("RS001", record.getRuleSetId());
        assertEquals(now, record.getTriageTime());
    }

    @Test
    void shouldCreateDeadLetterEventWithDefaultConstructor() {
        DeadLetterEvent event = new DeadLetterEvent();
        assertNull(event.getEventPayload());
        assertNull(event.getFailReason());
        assertNull(event.getFailTime());
        assertEquals("FAILED", event.getState());
        assertEquals(0, event.getRetryCount().intValue());
        assertEquals(3, event.getMaxRetryCount().intValue());
    }

    @Test
    void shouldSetAndGetDeadLetterEventFields() {
        DeadLetterEvent event = new DeadLetterEvent();
        event.setEventPayload("{\"sessionId\":\"s1\"}");
        event.setFailReason("连接超时");
        event.setFailTime(LocalDateTime.of(2026, 6, 29, 10, 0));
        event.setState("COMPENSATED");
        event.setRetryCount(2);
        event.setMaxRetryCount(5);

        assertEquals("{\"sessionId\":\"s1\"}", event.getEventPayload());
        assertEquals("连接超时", event.getFailReason());
        assertEquals(LocalDateTime.of(2026, 6, 29, 10, 0), event.getFailTime());
        assertEquals("COMPENSATED", event.getState());
        assertEquals(2, event.getRetryCount().intValue());
        assertEquals(5, event.getMaxRetryCount().intValue());
    }

    @Test
    void shouldCreateTriageRuleWithDefaultConstructor() {
        TriageRule rule = new TriageRule();
        assertNull(rule.getRuleId());
        assertNull(rule.getRuleSetId());
        assertNull(rule.getRuleVersion());
        assertNull(rule.getConditions());
        assertNull(rule.getResultDepartmentId());
        assertNull(rule.getResultDepartmentName());
        assertEquals(0.0f, rule.getScore(), 0.001f);
        assertTrue(rule.getEnabled());
        assertNull(rule.getCreateTime());
        assertNull(rule.getUpdateTime());
    }

    @Test
    void shouldSetAndGetTriageRuleFields() {
        TriageRule rule = new TriageRule();
        rule.setRuleId("R001");
        rule.setRuleSetId("RS001");
        rule.setRuleVersion("v1.0");
        rule.setConditions("{\"keyword\":\"头痛\"}");
        rule.setResultDepartmentId("dept-01");
        rule.setResultDepartmentName("神经内科");
        rule.setScore(0.9f);
        rule.setEnabled(false);
        LocalDateTime now = LocalDateTime.now();
        rule.setCreateTime(now);
        rule.setUpdateTime(now);

        assertEquals("R001", rule.getRuleId());
        assertEquals("RS001", rule.getRuleSetId());
        assertEquals("v1.0", rule.getRuleVersion());
        assertEquals("{\"keyword\":\"头痛\"}", rule.getConditions());
        assertEquals("dept-01", rule.getResultDepartmentId());
        assertEquals("神经内科", rule.getResultDepartmentName());
        assertEquals(0.9f, rule.getScore(), 0.001f);
        assertFalse(rule.getEnabled());
        assertEquals(now, rule.getCreateTime());
        assertEquals(now, rule.getUpdateTime());
    }
}
