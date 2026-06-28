package com.aimedical.modules.doctor.converter;

import com.aimedical.modules.doctor.dto.response.ConsultationQueueResponse;
import com.aimedical.modules.doctor.entity.ConsultationQueueEntity;
import com.aimedical.modules.doctor.entity.ConsultationStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ConsultationQueueConverterTest {

    private final ConsultationQueueConverter converter = new ConsultationQueueConverter();

    @Test
    void toResponse_shouldReturnNullWhenEntityIsNull() {
        assertNull(converter.toResponse(null));
    }

    @Test
    void toResponse_shouldMapAllFields() {
        ConsultationQueueEntity entity = new ConsultationQueueEntity();
        entity.setId(1L);
        entity.setPatientId(100L);
        entity.setPatientName("张三");
        entity.setDoctorId(200L);
        entity.setDepartment("内科");
        entity.setQueueNo("A001");
        entity.setStatus(ConsultationStatus.WAITING.getCode());
        entity.setRegisteredAt(LocalDateTime.of(2026, 6, 28, 10, 0));
        entity.setCalledAt(LocalDateTime.of(2026, 6, 28, 10, 30));
        entity.setFinishedAt(LocalDateTime.of(2026, 6, 28, 11, 0));
        entity.setRemark("测试备注");

        ConsultationQueueResponse response = converter.toResponse(entity);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(100L, response.patientId());
        assertEquals("张三", response.patientName());
        assertEquals(200L, response.doctorId());
        assertEquals("内科", response.department());
        assertEquals("A001", response.queueNo());
        assertEquals("WAITING", response.status());
        assertEquals(LocalDateTime.of(2026, 6, 28, 10, 0), response.registeredAt());
        assertEquals(LocalDateTime.of(2026, 6, 28, 10, 30), response.calledAt());
        assertEquals(LocalDateTime.of(2026, 6, 28, 11, 0), response.finishedAt());
        assertEquals("测试备注", response.remark());
    }
}
