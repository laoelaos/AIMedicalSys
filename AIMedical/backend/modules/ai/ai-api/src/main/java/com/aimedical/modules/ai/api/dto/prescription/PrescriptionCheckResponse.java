package com.aimedical.modules.ai.api.dto.prescription;

import java.util.List;

/**
 * AI 处方审核响应 DTO。
 *
 * <p>承载 AI 处方审核结果，包括风险等级、警告信息与是否通过。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public class PrescriptionCheckResponse {

    private String riskLevel;
    private List<String> warnings;
    private boolean passed;

    public PrescriptionCheckResponse() {
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }
}
