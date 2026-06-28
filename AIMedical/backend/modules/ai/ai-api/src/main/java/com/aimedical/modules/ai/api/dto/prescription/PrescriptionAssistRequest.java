package com.aimedical.modules.ai.api.dto.prescription;

/**
 * AI 辅助开方请求 DTO。
 *
 * <p>携带诊断与主诉，供 AI 辅助生成处方建议。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public class PrescriptionAssistRequest {

    private Long patientId;
    private String diagnosis;
    private String chiefComplaint;

    public PrescriptionAssistRequest() {
    }

    public PrescriptionAssistRequest(Long patientId, String diagnosis, String chiefComplaint) {
        this.patientId = patientId;
        this.diagnosis = diagnosis;
        this.chiefComplaint = chiefComplaint;
    }

    public Long getPatientId() {
        return patientId;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public String getChiefComplaint() {
        return chiefComplaint;
    }
}
