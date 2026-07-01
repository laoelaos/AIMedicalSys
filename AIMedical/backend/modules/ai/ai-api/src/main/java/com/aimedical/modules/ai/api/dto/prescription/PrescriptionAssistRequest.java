package com.aimedical.modules.ai.api.dto.prescription;

import java.util.List;

public class PrescriptionAssistRequest {

    private String diagnosis;
    private List<ExamResultItem> examResults;
    private PatientInfo patientInfo;
    private String existingPrescription;
    private String prescriptionId;
    private String encounterId;

    public PrescriptionAssistRequest() {
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public List<ExamResultItem> getExamResults() {
        return examResults;
    }

    public void setExamResults(List<ExamResultItem> examResults) {
        this.examResults = examResults;
    }

    public PatientInfo getPatientInfo() {
        return patientInfo;
    }

    public void setPatientInfo(PatientInfo patientInfo) {
        this.patientInfo = patientInfo;
    }

    public String getExistingPrescription() {
        return existingPrescription;
    }

    public void setExistingPrescription(String existingPrescription) {
        this.existingPrescription = existingPrescription;
    }

    public String getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(String prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(String encounterId) {
        this.encounterId = encounterId;
    }
}
