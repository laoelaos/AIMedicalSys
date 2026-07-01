package com.aimedical.modules.consultation.dialogue;

import com.aimedical.modules.consultation.dto.AdditionalResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DialogueSession {

    private String sessionId;
    private String chiefComplaint;
    private String correctedChiefComplaint;
    private List<AdditionalResponse> additionalResponses = new CopyOnWriteArrayList<>();
    private int aiFailCount = 0;
    private int roundCount = 0;
    private String ruleVersion;
    private String ruleSetId;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;

    public DialogueSession() {
    }

    public DialogueSession(String sessionId) {
        this.sessionId = sessionId;
        this.createdAt = LocalDateTime.now();
        this.lastAccessedAt = LocalDateTime.now();
    }

    public synchronized String getSessionId() {
        return sessionId;
    }

    public synchronized void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public synchronized String getChiefComplaint() {
        return chiefComplaint;
    }

    public synchronized void setChiefComplaint(String chiefComplaint) {
        this.chiefComplaint = chiefComplaint;
    }

    public synchronized String getCorrectedChiefComplaint() {
        return correctedChiefComplaint;
    }

    public synchronized void setCorrectedChiefComplaint(String correctedChiefComplaint) {
        this.correctedChiefComplaint = correctedChiefComplaint;
    }

    public synchronized List<AdditionalResponse> getAdditionalResponses() {
        return additionalResponses;
    }

    public synchronized void setAdditionalResponses(List<AdditionalResponse> additionalResponses) {
        this.additionalResponses = additionalResponses instanceof CopyOnWriteArrayList
                ? (CopyOnWriteArrayList<AdditionalResponse>) additionalResponses
                : new CopyOnWriteArrayList<>(additionalResponses);
    }

    public synchronized int getAiFailCount() {
        return aiFailCount;
    }

    public synchronized void setAiFailCount(int aiFailCount) {
        this.aiFailCount = aiFailCount;
    }

    public synchronized int getRoundCount() {
        return roundCount;
    }

    public synchronized void setRoundCount(int roundCount) {
        this.roundCount = roundCount;
    }

    public synchronized String getRuleVersion() {
        return ruleVersion;
    }

    public synchronized void setRuleVersion(String ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public synchronized String getRuleSetId() {
        return ruleSetId;
    }

    public synchronized void setRuleSetId(String ruleSetId) {
        this.ruleSetId = ruleSetId;
    }

    public synchronized LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public synchronized void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public synchronized LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public synchronized void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }
}
