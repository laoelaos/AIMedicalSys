package com.aimedical.modules.patient.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "allergy_history")
@Data
public class AllergyHistory extends BaseEntity {

    private Long healthProfileId;

    @Column(length = 255, nullable = false)
    private String allergen;

    @Column(length = 255)
    private String reactionType;

    @Column(length = 20)
    private String severity;

    private LocalDate firstOccurredAt;

    @Column(length = 500)
    private String note;

    @Column(length = 500)
    private String remark;

}
