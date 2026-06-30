package com.aimedical.modules.ai.api.dto.prescription;

import java.util.List;

/**
 * AI 辅助开方响应 DTO。
 *
 * <p>承载 AI 推荐的药品列表与综合建议。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public class PrescriptionAssistResponse {

    private List<RecommendedDrug> drugs;
    private String summary;

    public PrescriptionAssistResponse() {
    }

    public List<RecommendedDrug> getDrugs() {
        return drugs;
    }

    public void setDrugs(List<RecommendedDrug> drugs) {
        this.drugs = drugs;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * 推荐药品项。
     */
    public static class RecommendedDrug {

        private String drugName;
        private String specification;
        private String dosage;
        private String frequency;
        private String reason;

        public RecommendedDrug() {
        }

        public RecommendedDrug(String drugName, String specification, String dosage,
                               String frequency, String reason) {
            this.drugName = drugName;
            this.specification = specification;
            this.dosage = dosage;
            this.frequency = frequency;
            this.reason = reason;
        }

        public String getDrugName() {
            return drugName;
        }

        public void setDrugName(String drugName) {
            this.drugName = drugName;
        }

        public String getSpecification() {
            return specification;
        }

        public void setSpecification(String specification) {
            this.specification = specification;
        }

        public String getDosage() {
            return dosage;
        }

        public void setDosage(String dosage) {
            this.dosage = dosage;
        }

        public String getFrequency() {
            return frequency;
        }

        public void setFrequency(String frequency) {
            this.frequency = frequency;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
