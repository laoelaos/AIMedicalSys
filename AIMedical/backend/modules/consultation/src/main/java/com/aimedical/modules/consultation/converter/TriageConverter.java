package com.aimedical.modules.consultation.converter;

import com.aimedical.modules.ai.api.AiResult;
import com.aimedical.modules.ai.api.dto.triage.AdditionalResponseItem;
import com.aimedical.modules.ai.api.dto.triage.TriageRequest;
import com.aimedical.modules.consultation.dialogue.DialogueSession;
import com.aimedical.modules.consultation.dto.AdditionalResponse;
import com.aimedical.modules.consultation.dto.DialogueCreateRequest;
import com.aimedical.modules.consultation.dto.RecommendedDepartment;
import com.aimedical.modules.consultation.dto.RecommendedDoctor;
import com.aimedical.modules.consultation.dto.TriageResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TriageConverter {

    public TriageRequest toAiTriageRequest(DialogueCreateRequest request, DialogueSession session) {
        TriageRequest aiRequest = new TriageRequest();
        aiRequest.setChiefComplaint(request.getChiefComplaint());
        aiRequest.setPatientId(request.getPatientId());
        aiRequest.setSessionId(request.getSessionId());
        aiRequest.setRuleVersion(request.getRuleVersion());
        aiRequest.setRuleSetId(request.getRuleSetId());

        List<AdditionalResponseItem> items = new ArrayList<>();
        if (session != null && session.getAdditionalResponses() != null) {
            for (AdditionalResponse resp : session.getAdditionalResponses()) {
                AdditionalResponseItem item = new AdditionalResponseItem();
                item.setQuestion(resp.getQuestion());
                item.setAnswer(resp.getAnswer());
                item.setAnsweredAt(resp.getAnsweredAt());
                items.add(item);
            }
        }
        if (request.getAdditionalResponses() != null) {
            for (AdditionalResponse resp : request.getAdditionalResponses()) {
                AdditionalResponseItem item = new AdditionalResponseItem();
                item.setQuestion(resp.getQuestion());
                item.setAnswer(resp.getAnswer());
                item.setAnsweredAt(resp.getAnsweredAt());
                items.add(item);
            }
        }
        aiRequest.setAdditionalResponses(items);

        StringBuilder sb = new StringBuilder();
        for (AdditionalResponseItem item : items) {
            sb.append("Q: ").append(item.getQuestion() != null ? item.getQuestion() : "")
              .append(" A: ").append(item.getAnswer() != null ? item.getAnswer() : "")
              .append(" ");
        }
        if (sb.length() > 3000) {
            sb.setLength(3000);
            sb.append(" [TRUNCATED]");
        }
        aiRequest.setAdditionalResponsesText(sb.toString());

        if (session != null && session.getCorrectedChiefComplaint() != null) {
            aiRequest.setCorrectedChiefComplaint(session.getCorrectedChiefComplaint());
        }

        return aiRequest;
    }

    public TriageResponse toTriageResponse(
            AiResult<com.aimedical.modules.ai.api.dto.triage.TriageResponse> aiResult,
            List<RecommendedDoctor> doctors,
            DialogueSession session) {
        TriageResponse response = new TriageResponse();

        if (aiResult.isDegraded()) {
            response.setDegraded(true);
            response.setFallbackHint(aiResult.getFallbackReason());
        }

        com.aimedical.modules.ai.api.dto.triage.TriageResponse aiData = aiResult.getData();
        if (aiData != null) {
            response.setSessionId(aiData.getSessionId());
            response.setReason(aiData.getReason());
            response.setNeedFollowUp(aiData.isNeedFollowUp());
            response.setFollowUpQuestion(aiData.getFollowUpQuestion());
            response.setConfidence(aiData.getConfidence());

            if (aiData.getRecommendedDepartments() != null) {
                response.setDepartments(aiData.getRecommendedDepartments().stream()
                        .map(d -> new RecommendedDepartment(d.getDepartmentId(), d.getDepartmentName(), d.getScore()))
                        .collect(Collectors.toList()));
            } else {
                response.setDepartments(Collections.emptyList());
            }

            if (aiData.getMatchedRules() != null) {
                response.setMatchedRules(aiData.getMatchedRules().stream()
                        .map(r -> new com.aimedical.modules.consultation.dto.MatchedRule(
                                r.getRuleId(), r.getRuleName(), r.getScore()))
                        .collect(Collectors.toList()));
            }
        }

        response.setDoctors(doctors != null ? doctors : Collections.emptyList());

        if (session != null && aiData != null && aiData.getCorrectedChiefComplaint() != null) {
            session.setCorrectedChiefComplaint(aiData.getCorrectedChiefComplaint());
            response.setCorrectedChiefComplaint(aiData.getCorrectedChiefComplaint());
        }

        return response;
    }

    public TriageResponse toFallbackTriageResponse(
            List<RecommendedDepartment> departments,
            List<RecommendedDoctor> doctors,
            String sessionId,
            String reason,
            boolean ruleVersionMismatch,
            boolean fallbackHint) {
        TriageResponse response = new TriageResponse();
        response.setDepartments(departments != null ? departments : Collections.emptyList());
        response.setDoctors(doctors != null ? doctors : Collections.emptyList());
        response.setSessionId(sessionId);
        response.setReason(reason);
        response.setDegraded(true);
        response.setConfidence(null);
        response.setRuleVersionMismatch(ruleVersionMismatch);
        if (fallbackHint) {
            response.setFallbackHint("AI 服务持续不可用，建议稍后重试");
        }
        return response;
    }
}
