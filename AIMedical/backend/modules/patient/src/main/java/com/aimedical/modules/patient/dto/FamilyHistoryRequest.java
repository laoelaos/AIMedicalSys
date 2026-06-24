package com.aimedical.modules.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FamilyHistoryRequest {
    @NotBlank(message = "亲属关系不能为空")
    @Size(max = 50, message = "亲属关系不能超过50字符")
    private String relationship;
    @NotBlank(message = "疾病名称不能为空")
    @Size(max = 100, message = "疾病名称不能超过100字符")
    private String diseaseName;
    @Size(max = 200, message = "备注不能超过200字符")
    private String note;

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }
    public String getDiseaseName() { return diseaseName; }
    public void setDiseaseName(String diseaseName) { this.diseaseName = diseaseName; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
