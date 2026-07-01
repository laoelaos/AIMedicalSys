package com.aimedical.modules.ai.api.dto.prescription;

import java.util.List;

public class PrescriptionCheckRequest {

    private List<PrescriptionCheckItem> prescriptionItems;
    private PatientInfo patientInfo;
    private String prescriptionId;

    public PrescriptionCheckRequest() {
    }

    public List<PrescriptionCheckItem> getPrescriptionItems() {
        return prescriptionItems;
    }

    public void setPrescriptionItems(List<PrescriptionCheckItem> prescriptionItems) {
        this.prescriptionItems = prescriptionItems;
    }

    public PatientInfo getPatientInfo() {
        return patientInfo;
    }

    public void setPatientInfo(PatientInfo patientInfo) {
        this.patientInfo = patientInfo;
    }

    public String getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(String prescriptionId) {
        this.prescriptionId = prescriptionId;
    }
}
