package com.aimedical.modules.prescription.dto.assist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class DosageCheckRequest {

    private String prescriptionId;

    @NotBlank
    private String drugCode;

    @Positive
    private double dosage;

    @NotBlank
    private String unit;

    @NotBlank
    private String routeOfAdministration;

    private Integer patientAge;

    private BigDecimal patientWeight;

    private String frequency;

    public DosageCheckRequest() {
    }

    public String getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(String prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public String getDrugCode() {
        return drugCode;
    }

    public void setDrugCode(String drugCode) {
        this.drugCode = drugCode;
    }

    public double getDosage() {
        return dosage;
    }

    public void setDosage(double dosage) {
        this.dosage = dosage;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getRouteOfAdministration() {
        return routeOfAdministration;
    }

    public void setRouteOfAdministration(String routeOfAdministration) {
        this.routeOfAdministration = routeOfAdministration;
    }

    public Integer getPatientAge() {
        return patientAge;
    }

    public void setPatientAge(Integer patientAge) {
        this.patientAge = patientAge;
    }

    public BigDecimal getPatientWeight() {
        return patientWeight;
    }

    public void setPatientWeight(BigDecimal patientWeight) {
        this.patientWeight = patientWeight;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }
}
