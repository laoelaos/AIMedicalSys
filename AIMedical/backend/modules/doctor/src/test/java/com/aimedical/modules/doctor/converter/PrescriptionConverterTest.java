package com.aimedical.modules.doctor.converter;

import com.aimedical.modules.doctor.dto.request.PrescriptionItemRequest;
import com.aimedical.modules.doctor.dto.response.PrescriptionItemDto;
import com.aimedical.modules.doctor.dto.response.PrescriptionResponse;
import com.aimedical.modules.doctor.entity.PrescriptionEntity;
import com.aimedical.modules.doctor.entity.PrescriptionItemEntity;
import com.aimedical.modules.doctor.entity.PrescriptionStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrescriptionConverterTest {

    private final PrescriptionConverter converter = new PrescriptionConverter();

    @Test
    void toResponse_shouldReturnNullWhenEntityIsNull() {
        assertNull(converter.toResponse(null));
    }

    @Test
    void toResponse_shouldMapAllFieldsWithItems() {
        PrescriptionEntity entity = new PrescriptionEntity();
        entity.setId(1L);
        entity.setPatientId(100L);
        entity.setPatientName("张三");
        entity.setDoctorId(200L);
        entity.setDepartment("内科");
        entity.setStatus(PrescriptionStatus.DRAFT.getCode());
        entity.setDiagnosis("感冒");
        entity.setAiChecked(false);
        entity.setAiRiskLevel("LOW");
        entity.setAuditRemark(null);
        entity.setAuditedBy(null);
        entity.setAuditedAt(null);
        entity.setRemark("测试备注");
        entity.setCreatedAt(LocalDateTime.of(2026, 6, 28, 10, 0));
        entity.setUpdatedAt(LocalDateTime.of(2026, 6, 28, 10, 0));

        PrescriptionItemEntity item = new PrescriptionItemEntity();
        item.setId(10L);
        item.setDrugName("阿莫西林");
        item.setSpecification("0.25g");
        item.setDosage("2粒");
        item.setUsageMethod("口服");
        item.setFrequency("每日3次");
        item.setQuantity(new BigDecimal("6"));
        item.setUnit("粒");
        item.setRemark("饭后服用");
        entity.setItems(List.of(item));

        PrescriptionResponse response = converter.toResponse(entity);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(100L, response.patientId());
        assertEquals("张三", response.patientName());
        assertEquals(200L, response.doctorId());
        assertEquals("内科", response.department());
        assertEquals("DRAFT", response.status());
        assertEquals("感冒", response.diagnosis());
        assertFalse(response.aiChecked());
        assertEquals("LOW", response.aiRiskLevel());
        assertEquals("测试备注", response.remark());
        assertNotNull(response.items());
        assertEquals(1, response.items().size());

        PrescriptionItemDto itemDto = response.items().get(0);
        assertEquals(10L, itemDto.id());
        assertEquals("阿莫西林", itemDto.drugName());
        assertEquals("0.25g", itemDto.specification());
        assertEquals("2粒", itemDto.dosage());
        assertEquals("口服", itemDto.usageMethod());
        assertEquals("每日3次", itemDto.frequency());
        assertEquals(new BigDecimal("6"), itemDto.quantity());
        assertEquals("粒", itemDto.unit());
        assertEquals("饭后服用", itemDto.remark());
    }

    @Test
    void toResponse_shouldHandleNullItems() {
        PrescriptionEntity entity = new PrescriptionEntity();
        entity.setId(1L);
        entity.setItems(null);

        PrescriptionResponse response = converter.toResponse(entity);

        assertNotNull(response);
        assertNotNull(response.items());
        assertTrue(response.items().isEmpty());
    }

    @Test
    void toItemDto_shouldReturnNullWhenEntityIsNull() {
        assertNull(converter.toItemDto(null));
    }

    @Test
    void toItemEntity_shouldReturnNullWhenRequestIsNull() {
        assertNull(converter.toItemEntity(null));
    }

    @Test
    void toItemEntity_shouldMapAllFields() {
        PrescriptionItemRequest request = new PrescriptionItemRequest(
                "阿莫西林", "0.25g", "2粒", "口服", "每日3次",
                new BigDecimal("6"), "粒", "饭后服用");

        PrescriptionItemEntity entity = converter.toItemEntity(request);

        assertNotNull(entity);
        assertEquals("阿莫西林", entity.getDrugName());
        assertEquals("0.25g", entity.getSpecification());
        assertEquals("2粒", entity.getDosage());
        assertEquals("口服", entity.getUsageMethod());
        assertEquals("每日3次", entity.getFrequency());
        assertEquals(new BigDecimal("6"), entity.getQuantity());
        assertEquals("粒", entity.getUnit());
        assertEquals("饭后服用", entity.getRemark());
    }
}
