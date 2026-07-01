package com.aimedical.modules.consultation.dto;

public class MatchedRule {

    private String ruleId;
    private String ruleName;
    private float score;

    public MatchedRule() {
    }

    public MatchedRule(String ruleId, String ruleName, float score) {
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.score = score;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }
}
