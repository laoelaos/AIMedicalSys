package com.aimedical.modules.consultation.rule;

public interface TriageRuleEngine {

    MatchResult match(String chiefComplaint, String ruleVersion, String ruleSetId);

    String currentRuleVersion();

    String currentRuleSetId();
}
