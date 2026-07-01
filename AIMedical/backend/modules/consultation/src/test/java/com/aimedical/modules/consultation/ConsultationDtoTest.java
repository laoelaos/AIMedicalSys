package com.aimedical.modules.consultation;

import com.aimedical.modules.consultation.dto.AdditionalResponse;
import com.aimedical.modules.consultation.dto.DialogueCreateRequest;
import com.aimedical.modules.consultation.dto.MatchedRule;
import com.aimedical.modules.consultation.dto.RecommendedDepartment;
import com.aimedical.modules.consultation.dto.RecommendedDoctor;
import com.aimedical.modules.consultation.dto.TriageResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConsultationDtoTest {

    @Test
    void shouldCreateDialogueCreateRequestWithDefaultConstructor() {
        DialogueCreateRequest req = new DialogueCreateRequest();
        assertNull(req.getChiefComplaint());
        assertNull(req.getSessionId());
        assertNull(req.getPatientId());
        assertNull(req.getAge());
    }

    @Test
    void shouldSetAndGetDialogueCreateRequestFields() {
        DialogueCreateRequest req = new DialogueCreateRequest();
        req.setChiefComplaint("头痛三天");
        req.setPatientId("P001");
        req.setAge(30);
        req.setGender("M");
        req.setSessionId("session-001");
        req.setRuleVersion("v1.0");
        req.setRuleSetId("RS001");
        req.setCorrectedChiefComplaint("头痛三天，伴有恶心");
        assertEquals("头痛三天", req.getChiefComplaint());
        assertEquals("P001", req.getPatientId());
        assertEquals(30, req.getAge());
        assertEquals("M", req.getGender());
        assertEquals("session-001", req.getSessionId());
        assertEquals("v1.0", req.getRuleVersion());
        assertEquals("RS001", req.getRuleSetId());
        assertEquals("头痛三天，伴有恶心", req.getCorrectedChiefComplaint());
    }

    @Test
    void shouldCreateDialogueCreateRequestWithAllArgsConstructor() {
        List<AdditionalResponse> additions = new ArrayList<>();
        additions.add(new AdditionalResponse("q1", "a1", null));
        DialogueCreateRequest req = new DialogueCreateRequest("头痛", "P001", 30, "M",
                "session-001", "v1.0", "RS001", additions, "头痛三天");
        assertEquals("头痛", req.getChiefComplaint());
        assertEquals(1, req.getAdditionalResponses().size());
    }

    @Test
    void shouldSetAndGetAdditionalResponsesInDialogueCreateRequest() {
        DialogueCreateRequest req = new DialogueCreateRequest();
        List<AdditionalResponse> list = new ArrayList<>();
        list.add(new AdditionalResponse("q1", "a1", null));
        req.setAdditionalResponses(list);
        assertEquals(1, req.getAdditionalResponses().size());
        assertEquals("q1", req.getAdditionalResponses().get(0).getQuestion());
    }

    @Test
    void shouldCreateAdditionalResponseWithDefaultConstructor() {
        AdditionalResponse resp = new AdditionalResponse();
        assertNull(resp.getQuestion());
        assertNull(resp.getAnswer());
        assertNull(resp.getAnsweredAt());
    }

    @Test
    void shouldSetAndGetAdditionalResponseFields() {
        AdditionalResponse resp = new AdditionalResponse();
        resp.setQuestion("是否有过敏史？");
        resp.setAnswer("无");
        resp.setAnsweredAt("2026-06-29T10:00:00");
        assertEquals("是否有过敏史？", resp.getQuestion());
        assertEquals("无", resp.getAnswer());
        assertEquals("2026-06-29T10:00:00", resp.getAnsweredAt());
    }

    @Test
    void shouldCreateAdditionalResponseWithAllArgsConstructor() {
        AdditionalResponse resp = new AdditionalResponse("q", "a", "2026-06-29T10:00:00");
        assertEquals("q", resp.getQuestion());
        assertEquals("a", resp.getAnswer());
        assertEquals("2026-06-29T10:00:00", resp.getAnsweredAt());
    }

    @Test
    void shouldCreateTriageResponseWithDefaultConstructor() {
        TriageResponse resp = new TriageResponse();
        assertNull(resp.getDepartments());
        assertNull(resp.getDoctors());
        assertNull(resp.getReason());
        assertNull(resp.getSessionId());
        assertFalse(resp.isNeedFollowUp());
        assertNull(resp.getConfidence());
        assertFalse(resp.isDegraded());
        assertNull(resp.getFallbackHint());
        assertNull(resp.getRuleVersionMismatch());
    }

    @Test
    void shouldSetAndGetTriageResponseFields() {
        TriageResponse resp = new TriageResponse();
        resp.setReason("建议就诊神经内科");
        resp.setSessionId("session-001");
        resp.setNeedFollowUp(true);
        resp.setFollowUpQuestion("症状持续多久？");
        resp.setConfidence(0.85f);
        resp.setDegraded(true);
        resp.setFallbackHint("AI不可用");
        resp.setRuleVersionMismatch(true);
        assertEquals("建议就诊神经内科", resp.getReason());
        assertEquals("session-001", resp.getSessionId());
        assertTrue(resp.isNeedFollowUp());
        assertEquals("症状持续多久？", resp.getFollowUpQuestion());
        assertEquals(0.85f, resp.getConfidence(), 0.001f);
        assertTrue(resp.isDegraded());
        assertEquals("AI不可用", resp.getFallbackHint());
        assertTrue(resp.getRuleVersionMismatch());
    }

    @Test
    void shouldSetAndGetDepartmentsInTriageResponse() {
        TriageResponse resp = new TriageResponse();
        List<RecommendedDepartment> depts = new ArrayList<>();
        depts.add(new RecommendedDepartment("dept-01", "神经内科", 0.9f));
        resp.setDepartments(depts);
        assertEquals(1, resp.getDepartments().size());
        assertEquals("神经内科", resp.getDepartments().get(0).getDepartmentName());
    }

    @Test
    void shouldSetAndGetDoctorsInTriageResponse() {
        TriageResponse resp = new TriageResponse();
        List<RecommendedDoctor> doctors = new ArrayList<>();
        doctors.add(new RecommendedDoctor("D001", "张医生", "dept-01", 5, 0.95f));
        resp.setDoctors(doctors);
        assertEquals(1, resp.getDoctors().size());
        assertEquals("张医生", resp.getDoctors().get(0).getDoctorName());
    }

    @Test
    void shouldSetAndGetMatchedRulesInTriageResponse() {
        TriageResponse resp = new TriageResponse();
        List<MatchedRule> rules = new ArrayList<>();
        rules.add(new MatchedRule("R001", "头痛规则", 0.85f));
        resp.setMatchedRules(rules);
        assertEquals(1, resp.getMatchedRules().size());
        assertEquals("头痛规则", resp.getMatchedRules().get(0).getRuleName());
    }

    @Test
    void shouldDefaultRuleVersionMismatchToNull() {
        TriageResponse resp = new TriageResponse();
        assertNull(resp.getRuleVersionMismatch());
    }

    @Test
    void shouldSetRuleVersionMismatchToFalse() {
        TriageResponse resp = new TriageResponse();
        resp.setRuleVersionMismatch(false);
        assertNotNull(resp.getRuleVersionMismatch());
        assertFalse(resp.getRuleVersionMismatch());
    }

    @Test
    void shouldCreateRecommendedDepartmentWithDefaultConstructor() {
        RecommendedDepartment dept = new RecommendedDepartment();
        assertNull(dept.getDepartmentId());
        assertNull(dept.getDepartmentName());
        assertEquals(0.0f, dept.getScore(), 0.001f);
    }

    @Test
    void shouldSetAndGetRecommendedDepartmentFields() {
        RecommendedDepartment dept = new RecommendedDepartment();
        dept.setDepartmentId("dept-01");
        dept.setDepartmentName("神经内科");
        dept.setScore(0.9f);
        assertEquals("dept-01", dept.getDepartmentId());
        assertEquals("神经内科", dept.getDepartmentName());
        assertEquals(0.9f, dept.getScore(), 0.001f);
    }

    @Test
    void shouldCreateRecommendedDepartmentWithAllArgsConstructor() {
        RecommendedDepartment dept = new RecommendedDepartment("dept-01", "神经内科", 0.9f);
        assertEquals("dept-01", dept.getDepartmentId());
        assertEquals("神经内科", dept.getDepartmentName());
        assertEquals(0.9f, dept.getScore(), 0.001f);
    }

    @Test
    void shouldCreateRecommendedDoctorWithDefaultConstructor() {
        RecommendedDoctor doc = new RecommendedDoctor();
        assertNull(doc.getDoctorId());
        assertNull(doc.getDoctorName());
        assertNull(doc.getDepartmentId());
        assertEquals(0, doc.getAvailableSlotCount());
        assertEquals(0.0f, doc.getScore(), 0.001f);
    }

    @Test
    void shouldSetAndGetRecommendedDoctorFields() {
        RecommendedDoctor doc = new RecommendedDoctor();
        doc.setDoctorId("D001");
        doc.setDoctorName("张医生");
        doc.setDepartmentId("dept-01");
        doc.setAvailableSlotCount(5);
        doc.setScore(0.95f);
        assertEquals("D001", doc.getDoctorId());
        assertEquals("张医生", doc.getDoctorName());
        assertEquals("dept-01", doc.getDepartmentId());
        assertEquals(5, doc.getAvailableSlotCount());
        assertEquals(0.95f, doc.getScore(), 0.001f);
    }

    @Test
    void shouldCreateRecommendedDoctorWithAllArgsConstructor() {
        RecommendedDoctor doc = new RecommendedDoctor("D001", "张医生", "dept-01", 5, 0.95f);
        assertEquals("D001", doc.getDoctorId());
        assertEquals(5, doc.getAvailableSlotCount());
    }

    @Test
    void shouldCreateMatchedRuleWithDefaultConstructor() {
        MatchedRule rule = new MatchedRule();
        assertNull(rule.getRuleId());
        assertNull(rule.getRuleName());
        assertEquals(0.0f, rule.getScore(), 0.001f);
    }

    @Test
    void shouldSetAndGetMatchedRuleFields() {
        MatchedRule rule = new MatchedRule();
        rule.setRuleId("R001");
        rule.setRuleName("头痛规则");
        rule.setScore(0.85f);
        assertEquals("R001", rule.getRuleId());
        assertEquals("头痛规则", rule.getRuleName());
        assertEquals(0.85f, rule.getScore(), 0.001f);
    }

    @Test
    void shouldCreateMatchedRuleWithAllArgsConstructor() {
        MatchedRule rule = new MatchedRule("R001", "头痛规则", 0.85f);
        assertEquals("R001", rule.getRuleId());
        assertEquals(0.85f, rule.getScore(), 0.001f);
    }
}
