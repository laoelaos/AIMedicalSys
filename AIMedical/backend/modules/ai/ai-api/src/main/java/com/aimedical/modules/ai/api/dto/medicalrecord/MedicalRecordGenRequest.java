package com.aimedical.modules.ai.api.dto.medicalrecord;

/**
 * AI 病历生成请求 DTO。
 *
 * <p>携带主诉、现病史、既往史、诊断等结构化输入，供 AI 生成完整病历文本。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public class MedicalRecordGenRequest {

    private Long patientId;
    private Long templateId;
    private String chiefComplaint;
    private String presentIllness;
    private String pastHistory;
    private String diagnosis;

    public MedicalRecordGenRequest() {
    }

    public MedicalRecordGenRequest(Long patientId, Long templateId, String chiefComplaint,
                                   String presentIllness, String pastHistory, String diagnosis) {
        this.patientId = patientId;
        this.templateId = templateId;
        this.chiefComplaint = chiefComplaint;
        this.presentIllness = presentIllness;
        this.pastHistory = pastHistory;
        this.diagnosis = diagnosis;
    }

    public Long getPatientId() {
        return patientId;
    }

    public Long getTemplateId() {
        return templateId;
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

    public String getDiagnosis() {
        return diagnosis;
    }
}
