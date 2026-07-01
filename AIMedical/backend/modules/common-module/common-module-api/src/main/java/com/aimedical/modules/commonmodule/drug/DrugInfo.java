package com.aimedical.modules.commonmodule.drug;

public record DrugInfo(
    String drugCode,
    String drugName,
    String specification,
    String dosageForm,
    String manufacturer,
    String packageUnit
) {}
