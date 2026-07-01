package com.aimedical.modules.consultation;

import com.aimedical.modules.ai.api.AiResult;
import com.aimedical.modules.ai.api.dto.triage.AdditionalResponseItem;
import com.aimedical.modules.ai.api.dto.triage.RecommendedDepartment;
import com.aimedical.modules.ai.api.dto.triage.RecommendedDoctor;
import com.aimedical.modules.ai.api.dto.triage.TriageResponse;
import com.aimedical.modules.consultation.converter.TriageConverter;
import com.aimedical.modules.consultation.dialogue.DialogueSession;
import com.aimedical.modules.consultation.dto.AdditionalResponse;
import com.aimedical.modules.consultation.dto.DialogueCreateRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TriageConverterTest {

    private final TriageConverter converter = new TriageConverter();

    private static final String VALID_UUID = "550e8400-e29b-41d4-a716-446655440000";

    @Test
    void shouldConvertToAiTriageRequest() {
        DialogueCreateRequest req = new DialogueCreateRequest();
        req.setChiefComplaint("头痛三天");
        req.setPatientId("P001");
        req.setSessionId(VALID_UUID);
        req.setRuleVersion("v1.0");
        req.setRuleSetId("RS001");

        DialogueSession session = new DialogueSession(VALID_UUID);

        com.aimedical.modules.ai.api.dto.triage.TriageRequest aiRequest = converter.toAiTriageRequest(req, session);
        assertNotNull(aiRequest);
        assertEquals("头痛三天", aiRequest.getChiefComplaint());
        assertEquals("P001", aiRequest.getPatientId());
        assertEquals(VALID_UUID, aiRequest.getSessionId());
        assertEquals("v1.0", aiRequest.getRuleVersion());
        assertEquals("RS001", aiRequest.getRuleSetId());
    }

    @Test
    void shouldMergeAdditionalResponsesFromSessionAndRequest() {
        DialogueCreateRequest req = new DialogueCreateRequest();
        req.setChiefComplaint("头痛三天");

        List<AdditionalResponse> sessionResponses = new ArrayList<>();
        sessionResponses.add(new AdditionalResponse("既往病史？", "无", null));
        DialogueSession session = new DialogueSession(VALID_UUID);
        session.setAdditionalResponses(sessionResponses);

        List<AdditionalResponse> requestResponses = new ArrayList<>();
        requestResponses.add(new AdditionalResponse("过敏史？", "无", "2026-06-29T10:00:00"));
        req.setAdditionalResponses(requestResponses);

        com.aimedical.modules.ai.api.dto.triage.TriageRequest aiRequest = converter.toAiTriageRequest(req, session);
        assertEquals(2, aiRequest.getAdditionalResponses().size());
        assertEquals("既往病史？", aiRequest.getAdditionalResponses().get(0).getQuestion());
        assertEquals("过敏史？", aiRequest.getAdditionalResponses().get(1).getQuestion());
    }

    @Test
    void shouldConvertToTriageResponseWithAiData() {
        TriageResponse aiData = new TriageResponse();
        aiData.setSessionId(VALID_UUID);
        aiData.setReason("建议就诊神经内科");
        aiData.setNeedFollowUp(true);
        aiData.setFollowUpQuestion("症状持续多久？");
        aiData.setConfidence(0.85f);
        List<RecommendedDepartment> depts = new ArrayList<>();
        RecommendedDepartment dept = new RecommendedDepartment();
        dept.setDepartmentId("dept-01");
        dept.setDepartmentName("神经内科");
        dept.setScore(0.9f);
        depts.add(dept);
        aiData.setRecommendedDepartments(depts);

        aiData.setCorrectedChiefComplaint("修正后主诉：头痛疑似偏头痛");

        AiResult<TriageResponse> aiResult = AiResult.success(aiData);

        List<com.aimedical.modules.consultation.dto.RecommendedDoctor> doctors = new ArrayList<>();
        doctors.add(new com.aimedical.modules.consultation.dto.RecommendedDoctor("D001", "张医生", "dept-01", 5, 0.95f));

        DialogueSession session = new DialogueSession(VALID_UUID);
        com.aimedical.modules.consultation.dto.TriageResponse result = converter.toTriageResponse(aiResult, doctors, session);
        assertEquals(VALID_UUID, result.getSessionId());
        assertEquals("建议就诊神经内科", result.getReason());
        assertTrue(result.isNeedFollowUp());
        assertEquals("症状持续多久？", result.getFollowUpQuestion());
        assertEquals(0.85f, result.getConfidence(), 0.001f);
        assertFalse(result.isDegraded());
        assertEquals(1, result.getDepartments().size());
        assertEquals("神经内科", result.getDepartments().get(0).getDepartmentName());
        assertEquals(1, result.getDoctors().size());
        assertEquals("张医生", result.getDoctors().get(0).getDoctorName());
        assertEquals("修正后主诉：头痛疑似偏头痛", session.getCorrectedChiefComplaint());
        assertEquals("修正后主诉：头痛疑似偏头痛", result.getCorrectedChiefComplaint());
    }

    @Test
    void shouldPassCorrectedChiefComplaintFromSessionToAiRequest() {
        DialogueCreateRequest req = new DialogueCreateRequest();
        req.setChiefComplaint("头痛三天");
        req.setPatientId("P001");
        req.setSessionId(VALID_UUID);

        DialogueSession session = new DialogueSession(VALID_UUID);
        session.setCorrectedChiefComplaint("AI修正：偏头痛");

        com.aimedical.modules.ai.api.dto.triage.TriageRequest aiRequest = converter.toAiTriageRequest(req, session);
        assertEquals("AI修正：偏头痛", aiRequest.getCorrectedChiefComplaint());
    }

    @Test
    void shouldNotSetCorrectedChiefComplaintWhenSessionIsNull() {
        DialogueCreateRequest req = new DialogueCreateRequest();
        req.setChiefComplaint("头痛三天");
        req.setSessionId(VALID_UUID);

        com.aimedical.modules.ai.api.dto.triage.TriageRequest aiRequest = converter.toAiTriageRequest(req, null);
        assertNull(aiRequest.getCorrectedChiefComplaint());
    }

    @Test
    void shouldNotSetCorrectedChiefComplaintWhenSessionCcIsNull() {
        DialogueCreateRequest req = new DialogueCreateRequest();
        req.setChiefComplaint("头痛三天");
        req.setSessionId(VALID_UUID);

        DialogueSession session = new DialogueSession(VALID_UUID);

        com.aimedical.modules.ai.api.dto.triage.TriageRequest aiRequest = converter.toAiTriageRequest(req, session);
        assertNull(aiRequest.getCorrectedChiefComplaint());
    }

    @Test
    void shouldNotWriteBackCorrectedChiefComplaintWhenSessionIsNull() {
        TriageResponse aiData = new TriageResponse();
        aiData.setSessionId(VALID_UUID);
        aiData.setCorrectedChiefComplaint("修正后主诉");

        AiResult<TriageResponse> aiResult = AiResult.success(aiData);

        com.aimedical.modules.consultation.dto.TriageResponse result = converter.toTriageResponse(aiResult, null, null);
        assertNotNull(result);
    }

    @Test
    void shouldNotWriteBackCorrectedChiefComplaintWhenAiDataIsNull() {
        DialogueSession session = new DialogueSession(VALID_UUID);

        com.aimedical.modules.consultation.dto.TriageResponse result = converter.toTriageResponse(AiResult.failure("AI_UNAVAILABLE"), null, session);
        assertNull(session.getCorrectedChiefComplaint());
    }

    @Test
    void shouldMarkDegradedWhenAiResultIsDegraded() {
        AiResult<TriageResponse> degraded = AiResult.degraded("AI不可用");
        com.aimedical.modules.consultation.dto.TriageResponse result = converter.toTriageResponse(degraded, null, null);
        assertTrue(result.isDegraded());
        assertEquals("AI不可用", result.getFallbackHint());
    }

    @Test
    void shouldReturnEmptyListWhenAiDataHasNoDepartments() {
        TriageResponse aiData = new TriageResponse();
        aiData.setSessionId(VALID_UUID);
        aiData.setReason("reason");
        AiResult<TriageResponse> aiResult = AiResult.success(aiData);

        com.aimedical.modules.consultation.dto.TriageResponse result = converter.toTriageResponse(aiResult, new ArrayList<>(), null);
        assertNotNull(result.getDepartments());
        assertTrue(result.getDepartments().isEmpty());
        assertNotNull(result.getDoctors());
        assertTrue(result.getDoctors().isEmpty());
    }

    @Test
    void shouldReturnEmptyDepartmentsForNullAiData() {
        AiResult<TriageResponse> aiResult = AiResult.failure("AI_UNAVAILABLE");
        com.aimedical.modules.consultation.dto.TriageResponse result = converter.toTriageResponse(aiResult, null, null);
        assertNull(result.getDepartments());
    }

    @Test
    void shouldConcatenateItemsWithQAndAFormat() {
        DialogueCreateRequest req = new DialogueCreateRequest();
        req.setChiefComplaint("头痛");
        req.setSessionId(VALID_UUID);
        List<AdditionalResponse> responses = new ArrayList<>();
        responses.add(new AdditionalResponse("头痛多久？", "三天", null));
        responses.add(new AdditionalResponse("有恶心吗？", "没有", null));
        req.setAdditionalResponses(responses);

        com.aimedical.modules.ai.api.dto.triage.TriageRequest aiRequest = converter.toAiTriageRequest(req, new DialogueSession(VALID_UUID));
        assertEquals("Q: 头痛多久？ A: 三天 Q: 有恶心吗？ A: 没有 ", aiRequest.getAdditionalResponsesText());
    }

    @Test
    void shouldNotTruncateWhenExactly3000Chars() {
        DialogueCreateRequest req = new DialogueCreateRequest();
        req.setChiefComplaint("头痛");
        req.setSessionId(VALID_UUID);
        StringBuilder longAnswer = new StringBuilder();
        for (int i = 0; i < 2990; i++) longAnswer.append("a");
        List<AdditionalResponse> responses = new ArrayList<>();
        responses.add(new AdditionalResponse("Q?", longAnswer.toString(), null));
        req.setAdditionalResponses(responses);

        com.aimedical.modules.ai.api.dto.triage.TriageRequest aiRequest = converter.toAiTriageRequest(req, new DialogueSession(VALID_UUID));
        assertFalse(aiRequest.getAdditionalResponsesText().contains("[TRUNCATED]"));
        assertEquals(3000, aiRequest.getAdditionalResponsesText().length());
    }

    @Test
    void shouldTruncateWhenOver3000CharsAndAppendTruncatedMarker() {
        DialogueCreateRequest req = new DialogueCreateRequest();
        req.setChiefComplaint("头痛");
        req.setSessionId(VALID_UUID);
        StringBuilder longAnswer = new StringBuilder();
        for (int i = 0; i < 3000; i++) longAnswer.append("a");
        List<AdditionalResponse> responses = new ArrayList<>();
        responses.add(new AdditionalResponse("Q?", longAnswer.toString(), null));
        req.setAdditionalResponses(responses);

        com.aimedical.modules.ai.api.dto.triage.TriageRequest aiRequest = converter.toAiTriageRequest(req, new DialogueSession(VALID_UUID));
        int truncatedLength = 3000 + " [TRUNCATED]".length();
        assertTrue(aiRequest.getAdditionalResponsesText().endsWith(" [TRUNCATED]"));
        assertEquals(truncatedLength, aiRequest.getAdditionalResponsesText().length());
    }

    @Test
    void shouldReturnEmptyStringWhenItemsListIsEmpty() {
        DialogueCreateRequest req = new DialogueCreateRequest();
        req.setChiefComplaint("头痛");
        req.setSessionId(VALID_UUID);

        com.aimedical.modules.ai.api.dto.triage.TriageRequest aiRequest = converter.toAiTriageRequest(req, new DialogueSession(VALID_UUID));
        assertEquals("", aiRequest.getAdditionalResponsesText());
    }

    @Test
    void shouldHandleNullQuestionOrAnswerInItems() {
        DialogueCreateRequest req = new DialogueCreateRequest();
        req.setChiefComplaint("头痛");
        req.setSessionId(VALID_UUID);
        List<AdditionalResponse> responses = new ArrayList<>();
        responses.add(new AdditionalResponse(null, null, null));
        req.setAdditionalResponses(responses);

        com.aimedical.modules.ai.api.dto.triage.TriageRequest aiRequest = converter.toAiTriageRequest(req, new DialogueSession(VALID_UUID));
        assertEquals("Q:  A:  ", aiRequest.getAdditionalResponsesText());
    }

    @Test
    void shouldCreateDegradedFallbackResponse() {
        List<com.aimedical.modules.consultation.dto.RecommendedDepartment> depts = new ArrayList<>();
        depts.add(new com.aimedical.modules.consultation.dto.RecommendedDepartment("dept-01", "内科", 0.5f));
        List<com.aimedical.modules.consultation.dto.RecommendedDoctor> doctors = new ArrayList<>();
        doctors.add(new com.aimedical.modules.consultation.dto.RecommendedDoctor("doc-1", "Dr. A", "dept-01", 5, 0f));

        com.aimedical.modules.consultation.dto.TriageResponse result = converter.toFallbackTriageResponse(
                depts, doctors, "session-1", "AI不可用", false, false);

        assertTrue(result.isDegraded());
        assertNull(result.getConfidence());
        assertEquals("session-1", result.getSessionId());
        assertEquals("AI不可用", result.getReason());
        assertFalse(result.getRuleVersionMismatch());
        assertNull(result.getFallbackHint());
        assertEquals(1, result.getDepartments().size());
        assertEquals(1, result.getDoctors().size());
    }

    @Test
    void shouldSetFallbackHintWhenHintIsTrue() {
        com.aimedical.modules.consultation.dto.TriageResponse result = converter.toFallbackTriageResponse(
                new ArrayList<>(), new ArrayList<>(), "s1", "reason", false, true);

        assertEquals("AI 服务持续不可用，建议稍后重试", result.getFallbackHint());
    }

    @Test
    void shouldSetRuleVersionMismatchOnFallbackResponse() {
        com.aimedical.modules.consultation.dto.TriageResponse result = converter.toFallbackTriageResponse(
                new ArrayList<>(), new ArrayList<>(), "s1", "reason", true, false);

        assertTrue(result.getRuleVersionMismatch());
    }

    @Test
    void shouldHandleNullDepartmentsAndDoctorsInFallback() {
        com.aimedical.modules.consultation.dto.TriageResponse result = converter.toFallbackTriageResponse(
                null, null, "s1", "reason", false, false);

        assertNotNull(result.getDepartments());
        assertTrue(result.getDepartments().isEmpty());
        assertNotNull(result.getDoctors());
        assertTrue(result.getDoctors().isEmpty());
    }

    @Test
    void shouldNotSetMatchedRulesOnFallbackResponse() {
        com.aimedical.modules.consultation.dto.TriageResponse result = converter.toFallbackTriageResponse(
                new ArrayList<>(), new ArrayList<>(), "s1", "reason", false, false);

        assertNull(result.getMatchedRules());
    }

    @Test
    void shouldConvertMatchedRulesFromAiData() {
        TriageResponse aiData = new TriageResponse();
        aiData.setSessionId(VALID_UUID);
        aiData.setReason("reason");
        List<com.aimedical.modules.ai.api.dto.triage.MatchedRuleItem> rules = new ArrayList<>();
        com.aimedical.modules.ai.api.dto.triage.MatchedRuleItem ruleItem = new com.aimedical.modules.ai.api.dto.triage.MatchedRuleItem();
        ruleItem.setRuleId("R001");
        ruleItem.setRuleName("头痛规则");
        ruleItem.setScore(0.85f);
        rules.add(ruleItem);
        aiData.setMatchedRules(rules);

        AiResult<TriageResponse> aiResult = AiResult.success(aiData);
        com.aimedical.modules.consultation.dto.TriageResponse result = converter.toTriageResponse(aiResult, null, null);

        assertNotNull(result.getMatchedRules());
        assertEquals(1, result.getMatchedRules().size());
        assertEquals("R001", result.getMatchedRules().get(0).getRuleId());
        assertEquals("头痛规则", result.getMatchedRules().get(0).getRuleName());
        assertEquals(0.85f, result.getMatchedRules().get(0).getScore(), 0.001f);
    }
}
