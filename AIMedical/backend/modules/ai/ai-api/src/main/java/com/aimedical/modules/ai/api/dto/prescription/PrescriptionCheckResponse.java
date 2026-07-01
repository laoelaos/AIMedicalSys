package com.aimedical.modules.ai.api.dto.prescription;

import java.util.List;

public class PrescriptionCheckResponse {

    private String riskLevel;
    private List<AlertItem> alerts;
    private List<DrugInteractionItem> interactions;
    private List<SuggestionItem> suggestions;
    private boolean fromFallback;

    public PrescriptionCheckResponse() {
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<AlertItem> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<AlertItem> alerts) {
        this.alerts = alerts;
    }

    public List<DrugInteractionItem> getInteractions() {
        return interactions;
    }

    public void setInteractions(List<DrugInteractionItem> interactions) {
        this.interactions = interactions;
    }

    public List<SuggestionItem> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<SuggestionItem> suggestions) {
        this.suggestions = suggestions;
    }

    public boolean isFromFallback() {
        return fromFallback;
    }

    public void setFromFallback(boolean fromFallback) {
        this.fromFallback = fromFallback;
    }
}
