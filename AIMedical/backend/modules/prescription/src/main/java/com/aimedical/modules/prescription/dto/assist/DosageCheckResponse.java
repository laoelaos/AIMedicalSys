package com.aimedical.modules.prescription.dto.assist;

import java.util.List;

public class DosageCheckResponse {

    private List<DosageAlert> alerts;
    private String taskId;
    private Integer contextCriticalCount;
    private String prescriptionId;

    public DosageCheckResponse() {
    }

    public List<DosageAlert> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<DosageAlert> alerts) {
        this.alerts = alerts;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Integer getContextCriticalCount() {
        return contextCriticalCount;
    }

    public void setContextCriticalCount(Integer contextCriticalCount) {
        this.contextCriticalCount = contextCriticalCount;
    }

    public String getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(String prescriptionId) {
        this.prescriptionId = prescriptionId;
    }
}
