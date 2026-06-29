package com.aimedical.modules.patient.dto;

public class FamilyHistoryResponse {
    private Long id;
    private String relationship;
    private String diseaseName;
    private String note;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }
    public String getDiseaseName() { return diseaseName; }
    public void setDiseaseName(String diseaseName) { this.diseaseName = diseaseName; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
