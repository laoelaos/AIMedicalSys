package com.aimedical.modules.consultation.dto;

public class AdditionalResponse {

    private String question;
    private String answer;
    private String answeredAt;

    public AdditionalResponse() {
    }

    public AdditionalResponse(String question, String answer, String answeredAt) {
        this.question = question;
        this.answer = answer;
        this.answeredAt = answeredAt;
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
