package com.aimedical.modules.ai.api.dto.prescription;

import java.util.List;

public class PatientInfo {

    private String patientId;
    private Integer age;
    private String gender;
    private String allergyHistory;
    private List<AllergyDetailItem> allergyDetails;
    private List<String> comorbidities;
    private Double weight;

    public PatientInfo() {
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAllergyHistory() {
        return allergyHistory;
    }

    public void setAllergyHistory(String allergyHistory) {
        this.allergyHistory = allergyHistory;
    }

    public List<AllergyDetailItem> getAllergyDetails() {
        return allergyDetails;
    }

    public void setAllergyDetails(List<AllergyDetailItem> allergyDetails) {
        this.allergyDetails = allergyDetails;
    }

    public List<String> getComorbidities() {
        return comorbidities;
    }

    public void setComorbidities(List<String> comorbidities) {
        this.comorbidities = comorbidities;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }
}
