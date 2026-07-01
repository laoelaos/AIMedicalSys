package com.aimedical.modules.consultation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public class DialogueCreateRequest {

    @NotBlank
    @Size(min = 5, max = 500)
    private String chiefComplaint;

    private String patientId;
    private Integer age;
    private String gender;

    @NotBlank
    private String sessionId;

    private String ruleVersion;
    private String ruleSetId;
    private List<AdditionalResponse> additionalResponses;
    private String correctedChiefComplaint;

    public DialogueCreateRequest() {
    }

    public DialogueCreateRequest(String chiefComplaint, String patientId, Integer age, String gender,
                                  String sessionId, String ruleVersion, String ruleSetId,
                                  List<AdditionalResponse> additionalResponses, String correctedChiefComplaint) {
        this.chiefComplaint = chiefComplaint;
        this.patientId = patientId;
        this.age = age;
        this.gender = gender;
        this.sessionId = sessionId;
        this.ruleVersion = ruleVersion;
        this.ruleSetId = ruleSetId;
        this.additionalResponses = additionalResponses;
        this.correctedChiefComplaint = correctedChiefComplaint;
    }

    public String getChiefComplaint() {
        return chiefComplaint;
    }

    public void setChiefComplaint(String chiefComplaint) {
        this.chiefComplaint = chiefComplaint;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(String ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public String getRuleSetId() {
        return ruleSetId;
    }

    public void setRuleSetId(String ruleSetId) {
        this.ruleSetId = ruleSetId;
    }

    public List<AdditionalResponse> getAdditionalResponses() {
        return additionalResponses;
    }

    public void setAdditionalResponses(List<AdditionalResponse> additionalResponses) {
        this.additionalResponses = additionalResponses;
    }

    public String getCorrectedChiefComplaint() {
        return correctedChiefComplaint;
    }

    public void setCorrectedChiefComplaint(String correctedChiefComplaint) {
        this.correctedChiefComplaint = correctedChiefComplaint;
    }
}
