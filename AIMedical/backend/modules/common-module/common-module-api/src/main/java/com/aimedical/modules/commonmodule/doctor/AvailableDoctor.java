package com.aimedical.modules.commonmodule.doctor;

public record AvailableDoctor(
    String doctorId,
    String doctorName,
    String departmentId,
    int availableSlotCount
) {}
