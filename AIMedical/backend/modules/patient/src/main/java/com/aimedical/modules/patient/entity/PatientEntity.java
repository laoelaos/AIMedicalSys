package com.aimedical.modules.patient.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patient_profile")
@Getter
@Setter
public class PatientEntity extends BaseEntity {

    @Column(name = "user_id", unique = true, nullable = false)
    private Long userId;

    @Column(name = "real_name", length = 64, nullable = false)
    private String realName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "age")
    private Integer age;

    @Column(name = "id_card", length = 32)
    private String idCard;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "emergency_contact", length = 64)
    private String emergencyContact;

    @Column(name = "emergency_phone", length = 20)
    private String emergencyPhone;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(length = 20)
    private String phone;

    // CascadeType excludes REMOVE to avoid physical deletes bypassing @SQLDelete on BaseEntity;
    // child records are soft-deleted explicitly via Service-level repository.delete().
    @OneToMany(mappedBy = "patient", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<PatientAllergy> allergies = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<PatientChronicDisease> chronicDiseases = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<PatientFamilyHistory> familyHistories = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<PatientSurgeryHistory> surgeryHistories = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<PatientMedicationHistory> medicationHistories = new ArrayList<>();

    @Column(length = 2000)
    private String emergencyContact;
}
