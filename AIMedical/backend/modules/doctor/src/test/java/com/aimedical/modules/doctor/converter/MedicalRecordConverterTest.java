package com.aimedical.modules.doctor.converter;

import com.aimedical.modules.doctor.dto.response.MedicalRecordResponse;
import com.aimedical.modules.doctor.dto.response.MedicalRecordTemplateResponse;
import com.aimedical.modules.doctor.entity.MedicalRecordEntity;
import com.aimedical.modules.doctor.entity.MedicalRecordStatus;
import com.aimedical.modules.doctor.entity.MedicalRecordTemplateEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MedicalRecordConverterTest {

    private final MedicalRecordConverter converter = new MedicalRecordConverter();

    @Test
    void toResponse_shouldReturnNullWhenEntityIsNull() {
        assertNull(converter.toResponse(null));
    }

    @Test
    void toResponse_shouldMapAllFields() {
        MedicalRecordEntity entity = new MedicalRecordEntity();
        entity.setId(1L);
        entity.setPatientId(100L);
        entity.setDoctorId(200L);
        entity.setDepartment("内科");
        entity.setVersionNo(1);
        entity.setStatus(MedicalRecordStatus.OFFICIAL.getCode());
        entity.setChiefComplaint("头痛3天");
        entity.setPresentIllness("持续头痛");
        entity.setPastHistory("无特殊");
        entity.setDiagnosis("偏头痛");
        entity.setTreatmentPlan("对症治疗");
        entity.setPrescriptionId(50L);
        entity.setTemplateId(10L);
        entity.setAiGenerated(false);
        entity.setRemark("测试备注");
        entity.setCreatedAt(LocalDateTime.of(2026, 6, 28, 10, 0));
        entity.setUpdatedAt(LocalDateTime.of(2026, 6, 28, 10, 0));

        MedicalRecordResponse response = converter.toResponse(entity);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(100L, response.patientId());
        assertEquals(200L, response.doctorId());
        assertEquals("内科", response.department());
        assertEquals(1, response.versionNo());
        assertEquals("OFFICIAL", response.status());
        assertEquals("头痛3天", response.chiefComplaint());
        assertEquals("持续头痛", response.presentIllness());
        assertEquals("无特殊", response.pastHistory());
        assertEquals("偏头痛", response.diagnosis());
        assertEquals("对症治疗", response.treatmentPlan());
        assertEquals(50L, response.prescriptionId());
        assertEquals(10L, response.templateId());
        assertFalse(response.aiGenerated());
        assertEquals("测试备注", response.remark());
    }

    @Test
    void toTemplateResponse_shouldReturnNullWhenEntityIsNull() {
        assertNull(converter.toTemplateResponse(null));
    }

    @Test
    void toTemplateResponse_shouldMapAllFields() {
        MedicalRecordTemplateEntity entity = new MedicalRecordTemplateEntity();
        entity.setId(1L);
        entity.setDepartment("内科");
        entity.setName("内科初诊模板");
        entity.setChiefComplaintTpl("主诉模板");
        entity.setPresentIllnessTpl("现病史模板");
        entity.setPastHistoryTpl("既往史模板");
        entity.setDiagnosisTpl("诊断模板");
        entity.setTreatmentPlanTpl("治疗方案模板");
        entity.setEnabled(true);
        entity.setRemark("模板备注");

        MedicalRecordTemplateResponse response = converter.toTemplateResponse(entity);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("内科", response.department());
        assertEquals("内科初诊模板", response.name());
        assertEquals("主诉模板", response.chiefComplaintTpl());
        assertEquals("现病史模板", response.presentIllnessTpl());
        assertEquals("既往史模板", response.pastHistoryTpl());
        assertEquals("诊断模板", response.diagnosisTpl());
        assertEquals("治疗方案模板", response.treatmentPlanTpl());
        assertTrue(response.enabled());
        assertEquals("模板备注", response.remark());
    }
}
