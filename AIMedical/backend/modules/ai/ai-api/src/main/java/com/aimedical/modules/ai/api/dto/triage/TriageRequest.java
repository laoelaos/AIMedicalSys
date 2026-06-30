package com.aimedical.modules.ai.api.dto.triage;

import lombok.Data;

@Data
public class TriageRequest {

    private String chiefComplaint;

    public TriageRequest() {
    }
}
