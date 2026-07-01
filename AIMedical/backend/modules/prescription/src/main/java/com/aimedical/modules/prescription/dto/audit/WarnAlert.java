package com.aimedical.modules.prescription.dto.audit;

public class WarnAlert {
    private String alertCode;
    private String alertMessage;
    private AlertSeverity severity;

    public WarnAlert() {
    }

    public WarnAlert(String alertCode, String alertMessage, AlertSeverity severity) {
        this.alertCode = alertCode;
        this.alertMessage = alertMessage;
        this.severity = severity;
    }

    public String getAlertCode() {
        return alertCode;
    }

    public void setAlertCode(String alertCode) {
        this.alertCode = alertCode;
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AlertSeverity severity) {
        this.severity = severity;
    }
}
