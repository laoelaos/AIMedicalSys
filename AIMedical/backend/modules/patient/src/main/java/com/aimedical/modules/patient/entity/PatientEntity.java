package com.aimedical.modules.patient.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patient_profile")
public class PatientEntity extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private com.aimedical.modules.commonmodule.permission.User user;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PatientAllergy> allergies = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PatientChronicDisease> chronicDiseases = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PatientFamilyHistory> familyHistories = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PatientSurgeryHistory> surgeryHistories = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PatientMedicationHistory> medicationHistories = new ArrayList<>();

    @Column(length = 2000)
    private String emergencyContact;

    public com.aimedical.modules.commonmodule.permission.User getUser() {
        return user;
    }

    public void setUser(com.aimedical.modules.commonmodule.permission.User user) {
        this.user = user;
    }

    public List<PatientAllergy> getAllergies() {
        return allergies;
    }

    public void setAllergies(List<PatientAllergy> allergies) {
        this.allergies = allergies;
    }

    public List<PatientChronicDisease> getChronicDiseases() {
        return chronicDiseases;
    }

    public void setChronicDiseases(List<PatientChronicDisease> chronicDiseases) {
        this.chronicDiseases = chronicDiseases;
    }

    public List<PatientFamilyHistory> getFamilyHistories() {
        return familyHistories;
    }

    public void setFamilyHistories(List<PatientFamilyHistory> familyHistories) {
        this.familyHistories = familyHistories;
    }

    public List<PatientSurgeryHistory> getSurgeryHistories() {
        return surgeryHistories;
    }

    public void setSurgeryHistories(List<PatientSurgeryHistory> surgeryHistories) {
        this.surgeryHistories = surgeryHistories;
    }

    public List<PatientMedicationHistory> getMedicationHistories() {
        return medicationHistories;
    }

    public void setMedicationHistories(List<PatientMedicationHistory> medicationHistories) {
        this.medicationHistories = medicationHistories;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }
}
