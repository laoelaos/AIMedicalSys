package com.aimedical.modules.ai.api.dto.triage;

import java.time.LocalDateTime;

public class FollowUpItem {

    private String question;
    private String answer;
    private LocalDateTime answeredAt;

    public FollowUpItem() {
    }

    public String getQuestion() { return question; }
    public void setQuestion(String v) { this.question = v; }

    public String getAnswer() { return answer; }
    public void setAnswer(String v) { this.answer = v; }

    public LocalDateTime getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(LocalDateTime v) { this.answeredAt = v; }
}
