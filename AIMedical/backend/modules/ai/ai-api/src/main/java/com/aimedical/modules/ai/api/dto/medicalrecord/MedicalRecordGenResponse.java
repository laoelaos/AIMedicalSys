package com.aimedical.modules.ai.api.dto.medicalrecord;

/**
 * AI 病历生成响应 DTO。
 *
 * <p>承载 AI 生成的病历各段文本内容。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public class MedicalRecordGenResponse {

    private String chiefComplaint;
    private String presentIllness;
    private String pastHistory;
    private String diagnosis;
    private String treatmentPlan;

    public MedicalRecordGenResponse() {
    }

    public String getChiefComplaint() {
        return chiefComplaint;
    }

    public void setChiefComplaint(String chiefComplaint) {
        this.chiefComplaint = chiefComplaint;
    }

    public String getPresentIllness() {
        return presentIllness;
    }

    public void setPresentIllness(String presentIllness) {
        this.presentIllness = presentIllness;
    }

    public String getPastHistory() {
        return pastHistory;
    }

    public void setPastHistory(String pastHistory) {
        this.pastHistory = pastHistory;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getTreatmentPlan() {
        return treatmentPlan;
    }

    public void setTreatmentPlan(String treatmentPlan) {
        this.treatmentPlan = treatmentPlan;
    }
}
