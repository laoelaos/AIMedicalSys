package com.aimedical.modules.patient.dto;

import java.util.List;

public class HealthRecordSummaryResponse {
    private List<AllergyResponse> allergies;
    private List<ChronicDiseaseResponse> chronicDiseases;
    private List<FamilyHistoryResponse> familyHistories;
    private List<SurgeryHistoryResponse> surgeryHistories;
    private List<MedicationHistoryResponse> medicationHistories;

    public List<AllergyResponse> getAllergies() { return allergies; }
    public void setAllergies(List<AllergyResponse> allergies) { this.allergies = allergies; }
    public List<ChronicDiseaseResponse> getChronicDiseases() { return chronicDiseases; }
    public void setChronicDiseases(List<ChronicDiseaseResponse> chronicDiseases) { this.chronicDiseases = chronicDiseases; }
    public List<FamilyHistoryResponse> getFamilyHistories() { return familyHistories; }
    public void setFamilyHistories(List<FamilyHistoryResponse> familyHistories) { this.familyHistories = familyHistories; }
    public List<SurgeryHistoryResponse> getSurgeryHistories() { return surgeryHistories; }
    public void setSurgeryHistories(List<SurgeryHistoryResponse> surgeryHistories) { this.surgeryHistories = surgeryHistories; }
    public List<MedicationHistoryResponse> getMedicationHistories() { return medicationHistories; }
    public void setMedicationHistories(List<MedicationHistoryResponse> medicationHistories) { this.medicationHistories = medicationHistories; }
}
