package com.aimedical.modules.prescription.event;

public class DrugContraindicationChangeEvent extends DrugDictChangeEvent {

    public DrugContraindicationChangeEvent(ChangeType changeType, String drugCode) {
        super(changeType, drugCode);
    }
}
