package com.aimedical.modules.prescription.context;

public class DosageAlert {

    private String severity;
    private String message;
    private String drugCode;

    public DosageAlert() {
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDrugCode() {
        return drugCode;
    }

    public void setDrugCode(String drugCode) {
        this.drugCode = drugCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DosageAlert that = (DosageAlert) o;
        return java.util.Objects.equals(severity, that.severity)
                && java.util.Objects.equals(message, that.message)
                && java.util.Objects.equals(drugCode, that.drugCode);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(severity, message, drugCode);
    }
}
