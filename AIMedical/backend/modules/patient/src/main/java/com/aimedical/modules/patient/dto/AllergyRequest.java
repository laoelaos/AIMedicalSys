package com.aimedical.modules.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AllergyRequest {
    @NotBlank(message = "过敏原名称不能为空")
    @Size(max = 100, message = "过敏原名称不能超过100字符")
    private String allergen;

    @Size(max = 50, message = "反应类型不能超过50字符")
    private String reactionType;

    @Pattern(regexp = "^(MILD|MODERATE|SEVERE)?$", message = "严重程度取值必须为MILD、MODERATE或SEVERE")
    private String severity;

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为YYYY-MM-DD")
    private String occurredAt;

    public String getAllergen() { return allergen; }
    public void setAllergen(String allergen) { this.allergen = allergen; }
    public String getReactionType() { return reactionType; }
    public void setReactionType(String reactionType) { this.reactionType = reactionType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getOccurredAt() { return occurredAt; }
    public void setOccurredAt(String occurredAt) { this.occurredAt = occurredAt; }
}
