package com.aimedical.modules.ai.api.dto.prescription;

import java.util.List;

/**
 * AI 处方审核请求 DTO。
 *
 * <p>携带处方 ID、诊断与药品名称列表，供 AI 进行用药风险审核。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public class PrescriptionCheckRequest {

    private Long prescriptionId;
    private String diagnosis;
    private List<String> drugNames;

    public PrescriptionCheckRequest() {
    }

    public PrescriptionCheckRequest(Long prescriptionId, String diagnosis, List<String> drugNames) {
        this.prescriptionId = prescriptionId;
        this.diagnosis = diagnosis;
        this.drugNames = drugNames;
    }

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public List<String> getDrugNames() {
        return drugNames;
    }
}
