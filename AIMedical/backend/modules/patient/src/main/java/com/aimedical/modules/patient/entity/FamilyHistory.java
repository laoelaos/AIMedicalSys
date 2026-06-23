package com.aimedical.modules.patient.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "family_history")
@Data
public class FamilyHistory extends BaseEntity {

    private Long healthProfileId;

    @Column(length = 64, nullable = false)
    private String relationship;

    @Column(length = 255, nullable = false)
    private String diseaseName;

    @Column(length = 500)
    private String note;

    @Column(length = 500)
    private String remark;
}
