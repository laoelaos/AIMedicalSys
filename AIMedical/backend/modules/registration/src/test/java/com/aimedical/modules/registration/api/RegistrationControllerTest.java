package com.aimedical.modules.registration.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.registration.dto.CancelRegistrationRequest;
import com.aimedical.modules.registration.dto.RegistrationDTO;
import com.aimedical.modules.registration.dto.TriageRecordDTO;
import com.aimedical.modules.registration.entity.RegistrationStatus;
import com.aimedical.modules.registration.entity.TriageLevel;
import com.aimedical.modules.registration.service.RegistrationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RegistrationController 单元测试。
 * 通过 mock RegistrationService 验证各端点正确委派并返回 Result.success。
 */
@ExtendWith(MockitoExtension.class)
class RegistrationControllerTest {

    @Mock
    private RegistrationService registrationService;

    @InjectMocks
    private RegistrationController controller;

    @Test
    @DisplayName("POST / 创建挂号委派 service.createRegistration")
    void createRegistration() {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setId(1L);
        when(registrationService.createRegistration(any())).thenReturn(dto);

        Result<RegistrationDTO> result = controller.createRegistration(dto);

        assertEquals("SUCCESS", result.getCode());
        assertSame(dto, result.getData());
        verify(registrationService).createRegistration(dto);
    }

    @Test
    @DisplayName("GET /{id} 委派 service.getRegistration")
    void getRegistration() {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setId(2L);
        when(registrationService.getRegistration(2L)).thenReturn(dto);

        Result<RegistrationDTO> result = controller.getRegistration(2L);

        assertEquals("SUCCESS", result.getCode());
        assertSame(dto, result.getData());
        verify(registrationService).getRegistration(2L);
    }

    @Test
    @DisplayName("GET /patient/{patientId} 委派 service.getRegistrationsByPatient")
    void getByPatient() {
        Page<RegistrationDTO> page = new PageImpl<>(Collections.emptyList());
        Pageable pageable = PageRequest.of(0, 10);
        when(registrationService.getRegistrationsByPatient(eq(3L), any(Pageable.class))).thenReturn(page);

        Result<Page<RegistrationDTO>> result = controller.getByPatient(3L, pageable);

        assertEquals("SUCCESS", result.getCode());
        assertNotNull(result.getData());
        verify(registrationService).getRegistrationsByPatient(3L, pageable);
    }

    @Test
    @DisplayName("GET /doctor/{doctorId} 委派 service.getRegistrationsByDoctor")
    void getByDoctor() {
        Page<RegistrationDTO> page = new PageImpl<>(Collections.emptyList());
        Pageable pageable = PageRequest.of(0, 10);
        when(registrationService.getRegistrationsByDoctor(eq(4L), any(Pageable.class))).thenReturn(page);

        Result<Page<RegistrationDTO>> result = controller.getByDoctor(4L, pageable);

        assertEquals("SUCCESS", result.getCode());
        assertNotNull(result.getData());
        verify(registrationService).getRegistrationsByDoctor(4L, pageable);
    }

    @Test
    @DisplayName("POST /{id}/confirm 委派 service.confirmRegistration")
    void confirmRegistration() {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setId(5L);
        dto.setStatus(RegistrationStatus.CONFIRMED);
        when(registrationService.confirmRegistration(5L)).thenReturn(dto);

        Result<RegistrationDTO> result = controller.confirmRegistration(5L);

        assertEquals("SUCCESS", result.getCode());
        assertSame(dto, result.getData());
        verify(registrationService).confirmRegistration(5L);
    }

    @Test
    @DisplayName("POST /{id}/complete 委派 service.completeRegistration")
    void completeRegistration() {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setId(6L);
        when(registrationService.completeRegistration(6L)).thenReturn(dto);

        Result<RegistrationDTO> result = controller.completeRegistration(6L);

        assertEquals("SUCCESS", result.getCode());
        assertSame(dto, result.getData());
        verify(registrationService).completeRegistration(6L);
    }

    @Test
    @DisplayName("POST /{id}/cancel 委派 service.cancelRegistration")
    void cancelRegistration() {
        CancelRegistrationRequest req = new CancelRegistrationRequest();
        req.setCancelReason("时间冲突");
        RegistrationDTO dto = new RegistrationDTO();
        dto.setId(7L);
        when(registrationService.cancelRegistration(eq(7L), any(CancelRegistrationRequest.class))).thenReturn(dto);

        Result<RegistrationDTO> result = controller.cancelRegistration(7L, req);

        assertEquals("SUCCESS", result.getCode());
        assertSame(dto, result.getData());
        verify(registrationService).cancelRegistration(7L, req);
    }

    @Test
    @DisplayName("POST /{id}/noshow 委派 service.markNoShow")
    void markNoShow() {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setId(8L);
        when(registrationService.markNoShow(8L)).thenReturn(dto);

        Result<RegistrationDTO> result = controller.markNoShow(8L);

        assertEquals("SUCCESS", result.getCode());
        assertSame(dto, result.getData());
        verify(registrationService).markNoShow(8L);
    }

    @Test
    @DisplayName("POST /{registrationId}/triage 委派 service.createTriageRecord 并回填 registrationId")
    void createTriageRecord() {
        TriageRecordDTO dto = new TriageRecordDTO();
        dto.setTriageLevel(TriageLevel.LEVEL_3);
        when(registrationService.createTriageRecord(any(TriageRecordDTO.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Result<TriageRecordDTO> result = controller.createTriageRecord(9L, dto);

        assertEquals("SUCCESS", result.getCode());
        assertEquals(9L, result.getData().getRegistrationId());
        verify(registrationService).createTriageRecord(dto);
    }

    @Test
    @DisplayName("GET /{registrationId}/triage 委派 service.getTriageRecord")
    void getTriageRecord() {
        TriageRecordDTO dto = new TriageRecordDTO();
        dto.setRegistrationId(10L);
        when(registrationService.getTriageRecord(10L)).thenReturn(dto);

        Result<TriageRecordDTO> result = controller.getTriageRecord(10L);

        assertEquals("SUCCESS", result.getCode());
        assertSame(dto, result.getData());
        verify(registrationService).getTriageRecord(10L);
    }
}
