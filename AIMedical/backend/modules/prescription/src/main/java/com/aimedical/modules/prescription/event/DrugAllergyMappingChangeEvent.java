package com.aimedical.modules.prescription.event;

public class DrugAllergyMappingChangeEvent extends DrugDictChangeEvent {

    public DrugAllergyMappingChangeEvent(ChangeType changeType, String drugCode) {
        super(changeType, drugCode);
    }
}
