package com.aimedical.modules.prescription.rule.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "drug_contraindication_mapping")
public class DrugContraindicationMapping extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String drugCode;

    @Column(columnDefinition = "TEXT")
    private String contraindications;

    public DrugContraindicationMapping() {
    }

    public String getDrugCode() {
        return drugCode;
    }

    public void setDrugCode(String drugCode) {
        this.drugCode = drugCode;
    }

    public String getContraindications() {
        return contraindications;
    }

    public void setContraindications(String contraindications) {
        this.contraindications = contraindications;
    }
}
