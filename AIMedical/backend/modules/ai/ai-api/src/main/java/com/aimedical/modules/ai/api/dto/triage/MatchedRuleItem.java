package com.aimedical.modules.ai.api.dto.triage;

public class MatchedRuleItem {

    private String ruleId;
    private String ruleName;
    private float score;

    public MatchedRuleItem() {
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
