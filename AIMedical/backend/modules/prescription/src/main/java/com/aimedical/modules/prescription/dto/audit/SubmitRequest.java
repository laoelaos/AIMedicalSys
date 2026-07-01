package com.aimedical.modules.prescription.dto.audit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class SubmitRequest {

    @NotBlank
    private String prescriptionId;

    @NotEmpty
    private List<PrescriptionItem> prescriptionItems;

    private boolean forceSubmit;

    private Long auditRecordId;

    public SubmitRequest() {
    }

    public String getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(String prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public List<PrescriptionItem> getPrescriptionItems() {
        return prescriptionItems;
    }

    public void setPrescriptionItems(List<PrescriptionItem> prescriptionItems) {
        this.prescriptionItems = prescriptionItems;
    }

    public boolean isForceSubmit() {
        return forceSubmit;
    }

    public void setForceSubmit(boolean forceSubmit) {
        this.forceSubmit = forceSubmit;
    }

    public Long getAuditRecordId() {
        return auditRecordId;
    }

    public void setAuditRecordId(Long auditRecordId) {
        this.auditRecordId = auditRecordId;
    }
}
