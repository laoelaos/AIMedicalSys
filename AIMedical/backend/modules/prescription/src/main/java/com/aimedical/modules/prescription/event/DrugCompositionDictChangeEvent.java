package com.aimedical.modules.prescription.event;

public class DrugCompositionDictChangeEvent extends DrugDictChangeEvent {

    public DrugCompositionDictChangeEvent(ChangeType changeType, String drugCode) {
        super(changeType, drugCode);
    }
}
