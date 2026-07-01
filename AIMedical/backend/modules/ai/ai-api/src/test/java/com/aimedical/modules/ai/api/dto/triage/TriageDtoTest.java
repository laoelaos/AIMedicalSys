package com.aimedical.modules.ai.api.dto.triage;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TriageDtoTest {

    @Test
    void shouldCreateTriageRequestWithDefaultConstructor() {
        TriageRequest request = new TriageRequest();
        assertNull(request.getChiefComplaint());
    }

    @Test
    void shouldSetAndGetChiefComplaint() {
        TriageRequest request = new TriageRequest();
        request.setChiefComplaint("头痛三天");
        assertEquals("头痛三天", request.getChiefComplaint());
    }

    @Test
    void shouldCreateTriageResponseWithDefaultConstructor() {
        TriageResponse response = new TriageResponse();
        assertNull(response.getDepartments());
        assertNull(response.getReason());
    }

    @Test
    void shouldSetAndGetRecommendedDepartments() {
        TriageResponse response = new TriageResponse();
        List<RecommendedDepartment> depts = new ArrayList<>();
        depts.add(new RecommendedDepartment());
        response.setDepartments(depts);
        assertEquals(1, response.getDepartments().size());
    }

    @Test
    void shouldSetAndGetReason() {
        TriageResponse response = new TriageResponse();
        response.setReason("根据主诉推测为神经系统问题");
        assertEquals("根据主诉推测为神经系统问题", response.getReason());
    }

    @Test
    void shouldCreateRecommendedDepartmentWithDefaultConstructor() {
        RecommendedDepartment dept = new RecommendedDepartment();
        assertNull(dept.getDepartmentName());
    }

    @Test
    void shouldSetAndGetDepartmentName() {
        RecommendedDepartment dept = new RecommendedDepartment();
        dept.setDepartmentName("神经内科");
        assertEquals("神经内科", dept.getDepartmentName());
    }

    @Test
    void shouldBuildFullTriageResponseWithDepartments() {
        RecommendedDepartment neurology = new RecommendedDepartment();
        neurology.setDepartmentName("神经内科");

        RecommendedDepartment ent = new RecommendedDepartment();
        ent.setDepartmentName("耳鼻喉科");

        List<RecommendedDepartment> depts = new ArrayList<>();
        depts.add(neurology);
        depts.add(ent);

        TriageResponse response = new TriageResponse();
        response.setDepartments(depts);
        response.setReason("根据主诉头痛，建议优先就诊神经内科");

        assertEquals(2, response.getDepartments().size());
        assertEquals("神经内科", response.getDepartments().get(0).getDepartmentName());
        assertEquals("耳鼻喉科", response.getDepartments().get(1).getDepartmentName());
        assertEquals("根据主诉头痛，建议优先就诊神经内科", response.getReason());
    }

    @Test
    void shouldCreateAdditionalResponseItemWithDefaultConstructor() {
        AdditionalResponseItem item = new AdditionalResponseItem();
        assertNull(item.getQuestion());
        assertNull(item.getAnswer());
        assertNull(item.getAnsweredAt());
    }

    @Test
    void shouldSetAndGetAdditionalResponseItemFields() {
        AdditionalResponseItem item = new AdditionalResponseItem();
        item.setQuestion("是否有过敏史？");
        item.setAnswer("无");
        item.setAnsweredAt("2026-06-29T10:00:00");
        assertEquals("是否有过敏史？", item.getQuestion());
        assertEquals("无", item.getAnswer());
        assertEquals("2026-06-29T10:00:00", item.getAnsweredAt());
    }

    @Test
    void shouldCreateRecommendedDoctorWithDefaultConstructor() {
        RecommendedDoctor doctor = new RecommendedDoctor();
        assertNull(doctor.getDoctorId());
        assertNull(doctor.getDoctorName());
        assertNull(doctor.getDepartmentId());
        assertEquals(0, doctor.getAvailableSlotCount());
        assertEquals(0.0f, doctor.getScore(), 0.001f);
    }

    @Test
    void shouldSetAndGetRecommendedDoctorFields() {
        RecommendedDoctor doctor = new RecommendedDoctor();
        doctor.setDoctorId("D001");
        doctor.setDoctorName("张医生");
        doctor.setDepartmentId("dept-01");
        doctor.setAvailableSlotCount(5);
        doctor.setScore(0.95f);
        assertEquals("D001", doctor.getDoctorId());
        assertEquals("张医生", doctor.getDoctorName());
        assertEquals("dept-01", doctor.getDepartmentId());
        assertEquals(5, doctor.getAvailableSlotCount());
        assertEquals(0.95f, doctor.getScore(), 0.001f);
    }

    @Test
    void shouldCreateMatchedRuleItemWithDefaultConstructor() {
        MatchedRuleItem item = new MatchedRuleItem();
        assertNull(item.getRuleId());
        assertNull(item.getRuleName());
        assertEquals(0.0f, item.getScore(), 0.001f);
    }

    @Test
    void shouldSetAndGetMatchedRuleItemFields() {
        MatchedRuleItem item = new MatchedRuleItem();
        item.setRuleId("R001");
        item.setRuleName("头痛-神经内科规则");
        item.setScore(0.85f);
        assertEquals("R001", item.getRuleId());
        assertEquals("头痛-神经内科规则", item.getRuleName());
        assertEquals(0.85f, item.getScore(), 0.001f);
    }

    @Test
    void shouldSetAndGetAdditionalResponsesInTriageRequest() {
        TriageRequest request = new TriageRequest();
        List<AdditionalResponseItem> responses = new ArrayList<>();
        AdditionalResponseItem item = new AdditionalResponseItem();
        item.setQuestion("是否有过敏史？");
        item.setAnswer("无");
        responses.add(item);
        request.setAdditionalResponses(responses);
        assertEquals(1, request.getAdditionalResponses().size());
        assertEquals("是否有过敏史？", request.getAdditionalResponses().get(0).getQuestion());
    }

    @Test
    void shouldDefaultToNullForNewListFieldInTriageRequest() {
        TriageRequest request = new TriageRequest();
        assertNull(request.getAdditionalResponses());
    }

    @Test
    void shouldSetAndGetPatientId() {
        TriageRequest request = new TriageRequest();
        request.setPatientId("P001");
        assertEquals("P001", request.getPatientId());
    }

    @Test
    void shouldSetAndGetSessionIdInRequest() {
        TriageRequest request = new TriageRequest();
        request.setSessionId("session-001");
        assertEquals("session-001", request.getSessionId());
    }

    @Test
    void shouldSetAndGetRuleVersion() {
        TriageRequest request = new TriageRequest();
        request.setRuleVersion("v1.0");
        assertEquals("v1.0", request.getRuleVersion());
    }

    @Test
    void shouldSetAndGetRuleSetId() {
        TriageRequest request = new TriageRequest();
        request.setRuleSetId("RS001");
        assertEquals("RS001", request.getRuleSetId());
    }

    @Test
    void shouldSetAndGetRecommendedDoctors() {
        TriageResponse response = new TriageResponse();
        List<RecommendedDoctor> doctors = new ArrayList<>();
        RecommendedDoctor doctor = new RecommendedDoctor();
        doctor.setDoctorName("张医生");
        doctors.add(doctor);
        response.setRecommendedDoctors(doctors);
        assertEquals(1, response.getRecommendedDoctors().size());
        assertEquals("张医生", response.getRecommendedDoctors().get(0).getDoctorName());
    }

    @Test
    void shouldSetAndGetMatchedRules() {
        TriageResponse response = new TriageResponse();
        List<MatchedRuleItem> rules = new ArrayList<>();
        MatchedRuleItem rule = new MatchedRuleItem();
        rule.setRuleName("头痛规则");
        rules.add(rule);
        response.setMatchedRules(rules);
        assertEquals(1, response.getMatchedRules().size());
        assertEquals("头痛规则", response.getMatchedRules().get(0).getRuleName());
    }

    @Test
    void shouldSetAndGetAdditionalResponsesText() {
        TriageRequest request = new TriageRequest();
        request.setAdditionalResponsesText("Q: 头痛多久？ A: 三天");
        assertEquals("Q: 头痛多久？ A: 三天", request.getAdditionalResponsesText());
    }

    @Test
    void shouldDefaultToNullForAdditionalResponsesText() {
        TriageRequest request = new TriageRequest();
        assertNull(request.getAdditionalResponsesText());
    }

    @Test
    void shouldDefaultToFalseForNeedFollowUp() {
        TriageResponse response = new TriageResponse();
        assertFalse(response.isNeedFollowUp());
    }

    @Test
    void shouldSetAndGetNeedFollowUp() {
        TriageResponse response = new TriageResponse();
        response.setNeedFollowUp(true);
        assertTrue(response.isNeedFollowUp());
    }

    @Test
    void shouldSetAndGetFollowUpQuestion() {
        TriageResponse response = new TriageResponse();
        response.setFollowUpQuestion("症状是否持续超过三天？");
        assertEquals("症状是否持续超过三天？", response.getFollowUpQuestion());
    }

    @Test
    void shouldDefaultToNullForConfidence() {
        TriageResponse response = new TriageResponse();
        assertNull(response.getConfidence());
    }

    @Test
    void shouldSetAndGetConfidence() {
        TriageResponse response = new TriageResponse();
        response.setConfidence(0.85f);
        assertEquals(0.85f, response.getConfidence(), 0.001f);
    }

    @Test
    void shouldDefaultToFalseForDegraded() {
        TriageResponse response = new TriageResponse();
        assertFalse(response.isDegraded());
    }

    @Test
    void shouldSetAndGetDegradedInResponse() {
        TriageResponse response = new TriageResponse();
        response.setDegraded(true);
        assertTrue(response.isDegraded());
    }

    @Test
    void shouldSetAndGetSessionIdInResponse() {
        TriageResponse response = new TriageResponse();
        response.setSessionId("session-001");
        assertEquals("session-001", response.getSessionId());
    }

    @Test
    void shouldSetAndGetCorrectedChiefComplaintInResponse() {
        TriageResponse response = new TriageResponse();
        response.setCorrectedChiefComplaint("头痛三天，伴有恶心");
        assertEquals("头痛三天，伴有恶心", response.getCorrectedChiefComplaint());
    }

    @Test
    void shouldSetAndGetCorrectedChiefComplaintInRequest() {
        TriageRequest request = new TriageRequest();
        request.setCorrectedChiefComplaint("头痛三天，伴有恶心");
        assertEquals("头痛三天，伴有恶心", request.getCorrectedChiefComplaint());
    }

    @Test
    void shouldSetAndGetDepartmentId() {
        RecommendedDepartment dept = new RecommendedDepartment();
        dept.setDepartmentId("dept-01");
        assertEquals("dept-01", dept.getDepartmentId());
    }

    @Test
    void shouldSetAndGetScore() {
        RecommendedDepartment dept = new RecommendedDepartment();
        dept.setScore(0.9f);
        assertEquals(0.9f, dept.getScore(), 0.001f);
    }

    @Test
    void shouldHaveDefaultPrimitiveValueForScore() {
        RecommendedDepartment dept = new RecommendedDepartment();
        assertEquals(0.0f, dept.getScore(), 0.001f);
    }

    @Test
    void shouldBuildFullTriageResponseWithAllNewFields() {
        RecommendedDepartment dept = new RecommendedDepartment();
        dept.setDepartmentName("神经内科");
        dept.setDepartmentId("dept-01");
        dept.setScore(0.9f);

        RecommendedDoctor doctor = new RecommendedDoctor();
        doctor.setDoctorName("张医生");
        doctor.setScore(0.95f);

        MatchedRuleItem rule = new MatchedRuleItem();
        rule.setRuleName("头痛-神经内科规则");

        AdditionalResponseItem additional = new AdditionalResponseItem();
        additional.setQuestion("是否有过敏史？");
        additional.setAnswer("无");

        List<RecommendedDepartment> depts = List.of(dept);
        List<RecommendedDoctor> doctors = List.of(doctor);
        List<MatchedRuleItem> rules = List.of(rule);
        List<AdditionalResponseItem> additionals = List.of(additional);

        TriageRequest request = new TriageRequest();
        request.setChiefComplaint("头痛");
        request.setAdditionalResponses(additionals);
        request.setPatientId("P001");
        request.setSessionId("session-001");
        request.setRuleVersion("v1.0");
        request.setRuleSetId("RS001");
        request.setCorrectedChiefComplaint("头痛三天");

        TriageResponse response = new TriageResponse();
        response.setRecommendedDepartments(depts);
        response.setRecommendedDoctors(doctors);
        response.setMatchedRules(rules);
        response.setReason("建议就诊神经内科");
        response.setNeedFollowUp(true);
        response.setFollowUpQuestion("症状是否持续超过三天？");
        response.setConfidence(0.85f);
        response.setSessionId("session-001");
        response.setCorrectedChiefComplaint("头痛三天");

        assertEquals("头痛", request.getChiefComplaint());
        assertEquals(1, request.getAdditionalResponses().size());
        assertEquals("P001", request.getPatientId());
        assertEquals("session-001", request.getSessionId());
        assertEquals("v1.0", request.getRuleVersion());
        assertEquals("RS001", request.getRuleSetId());
        assertEquals("头痛三天", request.getCorrectedChiefComplaint());

        assertEquals(1, response.getRecommendedDepartments().size());
        assertEquals(1, response.getRecommendedDoctors().size());
        assertEquals(1, response.getMatchedRules().size());
        assertEquals("建议就诊神经内科", response.getReason());
        assertTrue(response.isNeedFollowUp());
        assertEquals("症状是否持续超过三天？", response.getFollowUpQuestion());
        assertEquals(0.85f, response.getConfidence(), 0.001f);
        assertEquals("session-001", response.getSessionId());
        assertEquals("头痛三天", response.getCorrectedChiefComplaint());
    }
}
