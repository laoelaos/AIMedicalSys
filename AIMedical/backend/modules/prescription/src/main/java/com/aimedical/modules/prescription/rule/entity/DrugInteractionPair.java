package com.aimedical.modules.prescription.rule.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "drug_interaction_pair", schema = "PHASE4_PRELOAD")
public class DrugInteractionPair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String drugCodeA;

    private String drugCodeB;

    private String severity;

    @Column(columnDefinition = "TEXT")
    private String description;

    public DrugInteractionPair() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDrugCodeA() {
        return drugCodeA;
    }

    public void setDrugCodeA(String drugCodeA) {
        this.drugCodeA = drugCodeA;
    }

    public String getDrugCodeB() {
        return drugCodeB;
    }

    public void setDrugCodeB(String drugCodeB) {
        this.drugCodeB = drugCodeB;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
