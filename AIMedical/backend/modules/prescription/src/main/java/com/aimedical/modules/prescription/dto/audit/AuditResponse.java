package com.aimedical.modules.prescription.dto.audit;

import com.aimedical.modules.prescription.service.audit.AuditRiskLevel;
import java.util.List;

public class AuditResponse {

    private AuditRiskLevel riskLevel;
    private List<AuditAlert> alerts;
    private List<DrugInteraction> interactions;
    private List<Suggestion> suggestions;
    private boolean fromFallback;

    public AuditResponse() {
    }

    public AuditRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(AuditRiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<AuditAlert> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<AuditAlert> alerts) {
        this.alerts = alerts;
    }

    public List<DrugInteraction> getInteractions() {
        return interactions;
    }

    public void setInteractions(List<DrugInteraction> interactions) {
        this.interactions = interactions;
    }

    public List<Suggestion> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<Suggestion> suggestions) {
        this.suggestions = suggestions;
    }

    public boolean isFromFallback() {
        return fromFallback;
    }

    public void setFromFallback(boolean fromFallback) {
        this.fromFallback = fromFallback;
    }
}
