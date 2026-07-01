package com.aimedical.modules.ai.api.dto.triage;

public class AdditionalResponseItem {

    private String question;
    private String answer;
    private String answeredAt;

    public AdditionalResponseItem() {
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(String answeredAt) {
        this.answeredAt = answeredAt;
    }
}
