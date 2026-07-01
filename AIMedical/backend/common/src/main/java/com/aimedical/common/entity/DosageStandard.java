package com.aimedical.common.entity;

import com.aimedical.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "dosage_standard", indexes = {
    @Index(name = "idx_dosage_drug_route", columnList = "drugCode, routeOfAdministration"),
    @Index(name = "idx_dosage_drug_route_age_weight", columnList = "drugCode, routeOfAdministration, ageRangeStart, ageRangeEnd, weightRangeStart, weightRangeEnd")
})
@Data
public class DosageStandard extends BaseEntity {

    @NotBlank
    @Column(nullable = false, length = 50)
    private String drugCode;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String routeOfAdministration;

    @Column
    private Integer ageRangeStart;

    @Column
    private Integer ageRangeEnd;

    @Column(precision = 10, scale = 2)
    private BigDecimal weightRangeStart;

    @Column(precision = 10, scale = 2)
    private BigDecimal weightRangeEnd;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal singleMax;

    @Column(precision = 12, scale = 3)
    private BigDecimal dailyMax;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String unit;
}
