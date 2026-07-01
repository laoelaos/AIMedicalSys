package com.aimedical.modules.prescription.dto.audit;

public class Suggestion {

    private String suggestionCode;
    private String suggestionText;

    public Suggestion() {
    }

    public String getSuggestionCode() {
        return suggestionCode;
    }

    public void setSuggestionCode(String suggestionCode) {
        this.suggestionCode = suggestionCode;
    }

    public String getSuggestionText() {
        return suggestionText;
    }

    public void setSuggestionText(String suggestionText) {
        this.suggestionText = suggestionText;
    }
}
