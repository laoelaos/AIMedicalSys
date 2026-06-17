package com.aimedical.modules.ai.api.dto.triage;

public class TriageRequest {

    private String chiefComplaint;

    public TriageRequest() {
    }

    public String getChiefComplaint() {
        return chiefComplaint;
    }

    public void setChiefComplaint(String chiefComplaint) {
        this.chiefComplaint = chiefComplaint;
    }
}
