package com.aimedical.modules.ai.api.dto.examination;

/**
 * AI 检查推荐请求 DTO。
 *
 * <p>携带诊断与主诉，供 AI 推荐检查项目。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public class ExaminationRecommendRequest {

    private Long patientId;
    private String diagnosis;
    private String chiefComplaint;

    public ExaminationRecommendRequest() {
    }

    public ExaminationRecommendRequest(Long patientId, String diagnosis, String chiefComplaint) {
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
