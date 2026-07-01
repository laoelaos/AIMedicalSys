package com.aimedical.modules.prescription.rule.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "drug_allergy_mapping")
public class DrugAllergyMapping extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String drugCode;

    @Column(columnDefinition = "TEXT")
    private String allergens;

    public DrugAllergyMapping() {
    }

    public String getDrugCode() {
        return drugCode;
    }

    public void setDrugCode(String drugCode) {
        this.drugCode = drugCode;
    }

    public String getAllergens() {
        return allergens;
    }

    public void setAllergens(String allergens) {
        this.allergens = allergens;
    }
}
