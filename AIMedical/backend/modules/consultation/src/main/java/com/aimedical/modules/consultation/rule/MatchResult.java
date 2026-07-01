package com.aimedical.modules.consultation.rule;

import com.aimedical.modules.consultation.dto.RecommendedDepartment;
import java.util.List;

public class MatchResult {

    private List<RecommendedDepartment> departments;
    private boolean ruleVersionMismatch;

    public MatchResult(List<RecommendedDepartment> departments, boolean ruleVersionMismatch) {
        this.departments = departments;
        this.ruleVersionMismatch = ruleVersionMismatch;
    }

    public List<RecommendedDepartment> getDepartments() {
        return departments;
    }

    public boolean isRuleVersionMismatch() {
        return ruleVersionMismatch;
    }
}
