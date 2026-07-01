package com.aimedical.modules.prescription.rule.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "drug_composition_dict")
public class DrugCompositionDict extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String drugCode;

    @Column(columnDefinition = "TEXT")
    private String ingredients;

    public DrugCompositionDict() {
    }

    public String getDrugCode() {
        return drugCode;
    }

    public void setDrugCode(String drugCode) {
        this.drugCode = drugCode;
    }

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }
}
