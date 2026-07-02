package com.aimedical.modules.ai.api.dto.diagnosis;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DiagnosisDtoTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        DiagnosisResponse response = new DiagnosisResponse();
        assertNull(response.getPossibleDiagnoses());
        assertNull(response.getSummary());
    }

    @Test
    void shouldSetAndGetPossibleDiagnoses() {
        DiagnosisResponse response = new DiagnosisResponse();
        List<String> diagnoses = new ArrayList<>();
        diagnoses.add("上呼吸道感染");
        diagnoses.add("流感");
        response.setPossibleDiagnoses(diagnoses);
        assertEquals(2, response.getPossibleDiagnoses().size());
        assertEquals("上呼吸道感染", response.getPossibleDiagnoses().get(0));
    }

    @Test
    void shouldSetAndGetSummary() {
        DiagnosisResponse response = new DiagnosisResponse();
        response.setSummary("综合患者症状，考虑上呼吸道感染可能性大");
        assertEquals("综合患者症状，考虑上呼吸道感染可能性大", response.getSummary());
    }

    @Test
    void shouldBuildFullDiagnosisResponse() {
        List<String> diagnoses = new ArrayList<>();
        diagnoses.add("高血压");
        diagnoses.add("冠心病");

        DiagnosisResponse response = new DiagnosisResponse();
        response.setPossibleDiagnoses(diagnoses);
        response.setSummary("需进一步检查心电图");

        assertEquals(2, response.getPossibleDiagnoses().size());
        assertEquals("高血压", response.getPossibleDiagnoses().get(0));
        assertEquals("冠心病", response.getPossibleDiagnoses().get(1));
        assertEquals("需进一步检查心电图", response.getSummary());
    }
}
