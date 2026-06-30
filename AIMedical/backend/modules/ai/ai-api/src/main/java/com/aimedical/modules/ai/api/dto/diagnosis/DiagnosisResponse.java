package com.aimedical.modules.ai.api.dto.diagnosis;

import java.util.List;

/**
 * AI 诊断响应 DTO。
 *
 * <p>承载 AI 辅助诊断结果，包括可能的诊断列表与综合建议。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public class DiagnosisResponse {

    private List<String> possibleDiagnoses;
    private String summary;

    public DiagnosisResponse() {
    }

    public List<String> getPossibleDiagnoses() {
        return possibleDiagnoses;
    }

    public void setPossibleDiagnoses(List<String> possibleDiagnoses) {
        this.possibleDiagnoses = possibleDiagnoses;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
