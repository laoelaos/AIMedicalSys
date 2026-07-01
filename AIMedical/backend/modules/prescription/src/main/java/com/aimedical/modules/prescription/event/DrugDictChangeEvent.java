package com.aimedical.modules.prescription.event;

public abstract class DrugDictChangeEvent {

    public enum ChangeType { CREATE, UPDATE, DELETE }

    private final ChangeType changeType;
    private final String drugCode;

    public DrugDictChangeEvent(ChangeType changeType, String drugCode) {
        this.changeType = changeType;
        this.drugCode = drugCode;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public String getDrugCode() {
        return drugCode;
    }
}
