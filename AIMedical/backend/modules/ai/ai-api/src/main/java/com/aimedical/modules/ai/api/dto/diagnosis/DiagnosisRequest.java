package com.aimedical.modules.ai.api.dto.diagnosis;

/**
 * AI 诊断请求 DTO。
 *
 * <p>携带前端录入的主诉、现病史、既往史等结构化数据，供 AI 进行辅助诊断。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public class DiagnosisRequest {

    private Long patientId;
    private String chiefComplaint;
    private String presentIllness;
    private String pastHistory;

    public DiagnosisRequest() {
    }

    public DiagnosisRequest(Long patientId, String chiefComplaint, String presentIllness, String pastHistory) {
        this.patientId = patientId;
        this.chiefComplaint = chiefComplaint;
        this.presentIllness = presentIllness;
        this.pastHistory = pastHistory;
    }

    public Long getPatientId() {
        return patientId;
    }

    public String getChiefComplaint() {
        return chiefComplaint;
    }

    public String getPresentIllness() {
        return presentIllness;
    }

    public String getPastHistory() {
        return pastHistory;
    }
}
