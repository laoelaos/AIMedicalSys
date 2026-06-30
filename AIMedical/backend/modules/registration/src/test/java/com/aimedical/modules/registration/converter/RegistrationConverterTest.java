package com.aimedical.modules.registration.converter;

import com.aimedical.modules.registration.dto.RegistrationDTO;
import com.aimedical.modules.registration.dto.TriageRecordDTO;
import com.aimedical.modules.registration.entity.Registration;
import com.aimedical.modules.registration.entity.RegistrationStatus;
import com.aimedical.modules.registration.entity.RegistrationType;
import com.aimedical.modules.registration.entity.TriageLevel;
import com.aimedical.modules.registration.entity.TriageRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * RegistrationConverter 单元测试。
 * 覆盖四个静态方法（含 null 与非 null 分支）以及全部字段映射。
 */
class RegistrationConverterTest {

    @Test
    @DisplayName("toRegistrationDTO(null) 返回 null")
    void toRegistrationDTO_null() {
        assertNull(RegistrationConverter.toRegistrationDTO(null));
    }

    @Test
    @DisplayName("toRegistrationDTO 完整字段映射")
    void toRegistrationDTO_fullMapping() {
        Registration entity = new Registration();
        entity.setId(1L);
        entity.setPatientId(10L);
        entity.setDoctorId(20L);
        entity.setRegistrationType(RegistrationType.OUTPATIENT);
        entity.setDepartment("内科");
        entity.setScheduledDate(LocalDate.of(2026, 1, 1));
        entity.setScheduledTimeSlot("09:00-10:00");
        entity.setStatus(RegistrationStatus.PENDING);
        entity.setCancelReason("原因");
        entity.setCancelTime(LocalDateTime.of(2026, 1, 1, 8, 0));
        entity.setCancelType("online");
        entity.setTriageLevel(TriageLevel.LEVEL_2);
        entity.setChiefComplaint("头痛");
        entity.setRegistrationFee(new BigDecimal("50.00"));
        entity.setQueueNumber(5);
        entity.setRemark("备注");
        entity.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        entity.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 1, 0));

        RegistrationDTO dto = RegistrationConverter.toRegistrationDTO(entity);

        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getPatientId());
        assertEquals(20L, dto.getDoctorId());
        assertEquals(RegistrationType.OUTPATIENT, dto.getRegistrationType());
        assertEquals("内科", dto.getDepartment());
        assertEquals(LocalDate.of(2026, 1, 1), dto.getScheduledDate());
        assertEquals("09:00-10:00", dto.getScheduledTimeSlot());
        assertEquals(RegistrationStatus.PENDING, dto.getStatus());
        assertEquals("原因", dto.getCancelReason());
        assertEquals(LocalDateTime.of(2026, 1, 1, 8, 0), dto.getCancelTime());
        assertEquals("online", dto.getCancelType());
        assertEquals(TriageLevel.LEVEL_2, dto.getTriageLevel());
        assertEquals("头痛", dto.getChiefComplaint());
        assertEquals(new BigDecimal("50.00"), dto.getRegistrationFee());
        assertEquals(5, dto.getQueueNumber());
        assertEquals("备注", dto.getRemark());
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), dto.getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 1, 1, 1, 0), dto.getUpdatedAt());
    }

    @Test
    @DisplayName("toRegistrationEntity(null) 返回 null")
    void toRegistrationEntity_null() {
        assertNull(RegistrationConverter.toRegistrationEntity(null));
    }

    @Test
    @DisplayName("toRegistrationEntity 完整字段映射")
    void toRegistrationEntity_fullMapping() {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setPatientId(10L);
        dto.setDoctorId(20L);
        dto.setRegistrationType(RegistrationType.EXAMINATION);
        dto.setDepartment("检验科");
        dto.setScheduledDate(LocalDate.of(2026, 1, 1));
        dto.setScheduledTimeSlot("09:00-10:00");
        dto.setStatus(RegistrationStatus.CONFIRMED);
        dto.setCancelReason("原因");
        dto.setCancelTime(LocalDateTime.of(2026, 1, 1, 8, 0));
        dto.setCancelType("offline");
        dto.setTriageLevel(TriageLevel.LEVEL_3);
        dto.setChiefComplaint("发热");
        dto.setRegistrationFee(new BigDecimal("80.00"));
        dto.setQueueNumber(7);
        dto.setRemark("备注");

        Registration entity = RegistrationConverter.toRegistrationEntity(dto);

        assertEquals(10L, entity.getPatientId());
        assertEquals(20L, entity.getDoctorId());
        assertEquals(RegistrationType.EXAMINATION, entity.getRegistrationType());
        assertEquals("检验科", entity.getDepartment());
        assertEquals(LocalDate.of(2026, 1, 1), entity.getScheduledDate());
        assertEquals("09:00-10:00", entity.getScheduledTimeSlot());
        assertEquals(RegistrationStatus.CONFIRMED, entity.getStatus());
        assertEquals("原因", entity.getCancelReason());
        assertEquals(LocalDateTime.of(2026, 1, 1, 8, 0), entity.getCancelTime());
        assertEquals("offline", entity.getCancelType());
        assertEquals(TriageLevel.LEVEL_3, entity.getTriageLevel());
        assertEquals("发热", entity.getChiefComplaint());
        assertEquals(new BigDecimal("80.00"), entity.getRegistrationFee());
        assertEquals(7, entity.getQueueNumber());
        assertEquals("备注", entity.getRemark());
    }

    @Test
    @DisplayName("toTriageRecordDTO(null) 返回 null")
    void toTriageRecordDTO_null() {
        assertNull(RegistrationConverter.toTriageRecordDTO(null));
    }

    @Test
    @DisplayName("toTriageRecordDTO 完整字段映射")
    void toTriageRecordDTO_fullMapping() {
        TriageRecord entity = new TriageRecord();
        entity.setId(1L);
        entity.setRegistrationId(100L);
        entity.setPatientId(10L);
        entity.setNurseId(20L);
        entity.setSymptoms("咳嗽");
        entity.setTemperature(new BigDecimal("38.5"));
        entity.setBloodPressure("120/80");
        entity.setHeartRate(90);
        entity.setTriageDepartment("急诊科");
        entity.setTriageLevel(TriageLevel.LEVEL_2);
        entity.setTriageNote("需观察");
        entity.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        entity.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 1, 0));

        TriageRecordDTO dto = RegistrationConverter.toTriageRecordDTO(entity);

        assertEquals(1L, dto.getId());
        assertEquals(100L, dto.getRegistrationId());
        assertEquals(10L, dto.getPatientId());
        assertEquals(20L, dto.getNurseId());
        assertEquals("咳嗽", dto.getSymptoms());
        assertEquals(new BigDecimal("38.5"), dto.getTemperature());
        assertEquals("120/80", dto.getBloodPressure());
        assertEquals(90, dto.getHeartRate());
        assertEquals("急诊科", dto.getTriageDepartment());
        assertEquals(TriageLevel.LEVEL_2, dto.getTriageLevel());
        assertEquals("需观察", dto.getTriageNote());
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), dto.getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 1, 1, 1, 0), dto.getUpdatedAt());
    }

    @Test
    @DisplayName("toTriageRecordEntity(null) 返回 null")
    void toTriageRecordEntity_null() {
        assertNull(RegistrationConverter.toTriageRecordEntity(null));
    }

    @Test
    @DisplayName("toTriageRecordEntity 完整字段映射")
    void toTriageRecordEntity_fullMapping() {
        TriageRecordDTO dto = new TriageRecordDTO();
        dto.setRegistrationId(100L);
        dto.setPatientId(10L);
        dto.setNurseId(20L);
        dto.setSymptoms("咳嗽");
        dto.setTemperature(new BigDecimal("38.5"));
        dto.setBloodPressure("120/80");
        dto.setHeartRate(90);
        dto.setTriageDepartment("急诊科");
        dto.setTriageLevel(TriageLevel.LEVEL_2);
        dto.setTriageNote("需观察");

        TriageRecord entity = RegistrationConverter.toTriageRecordEntity(dto);

        assertEquals(100L, entity.getRegistrationId());
        assertEquals(10L, entity.getPatientId());
        assertEquals(20L, entity.getNurseId());
        assertEquals("咳嗽", entity.getSymptoms());
        assertEquals(new BigDecimal("38.5"), entity.getTemperature());
        assertEquals("120/80", entity.getBloodPressure());
        assertEquals(90, entity.getHeartRate());
        assertEquals("急诊科", entity.getTriageDepartment());
        assertEquals(TriageLevel.LEVEL_2, entity.getTriageLevel());
        assertEquals("需观察", entity.getTriageNote());
    }
}
