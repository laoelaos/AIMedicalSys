package com.aimedical.modules.doctor.service.impl;

import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.common.result.Result;
import com.aimedical.modules.doctor.converter.ConsultationQueueConverter;
import com.aimedical.modules.doctor.dto.response.ConsultationQueueResponse;
import com.aimedical.modules.doctor.entity.ConsultationQueueEntity;
import com.aimedical.modules.doctor.entity.ConsultationStatus;
import com.aimedical.modules.doctor.repository.ConsultationQueueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultationQueueServiceImplTest {

    @Mock
    private ConsultationQueueRepository queueRepository;

    @Mock
    private ConsultationQueueConverter converter;

    @InjectMocks
    private ConsultationQueueServiceImpl service;

    private ConsultationQueueEntity buildEntity(Long id, String status, Long doctorId) {
        ConsultationQueueEntity entity = new ConsultationQueueEntity();
        entity.setId(id);
        entity.setPatientId(100L);
        entity.setPatientName("张三");
        entity.setDoctorId(doctorId);
        entity.setDepartment("内科");
        entity.setQueueNo("A001");
        entity.setStatus(status);
        entity.setRegisteredAt(LocalDateTime.now());
        return entity;
    }

    private ConsultationQueueResponse buildResponse(Long id) {
        return new ConsultationQueueResponse(id, 100L, "张三", 200L, "内科",
                "A001", "WAITING", LocalDateTime.now(), null, null, null);
    }

    @Test
    void listMyQueue_shouldReturnActiveStatuses() {
        ConsultationQueueEntity entity = buildEntity(1L, ConsultationStatus.WAITING.getCode(), 200L);
        ConsultationQueueResponse response = buildResponse(1L);
        when(queueRepository.findByDoctorIdAndStatusInOrderByRegisteredAtAsc(eq(200L), anyList()))
                .thenReturn(List.of(entity));
        when(converter.toResponse(entity)).thenReturn(response);

        Result<List<ConsultationQueueResponse>> result = service.listMyQueue(200L);

        assertEquals("SUCCESS", result.getCode());
        assertEquals(1, result.getData().size());
        verify(queueRepository).findByDoctorIdAndStatusInOrderByRegisteredAtAsc(eq(200L), anyList());
    }

    @Test
    void listWaiting_shouldReturnOnlyWaitingStatus() {
        ConsultationQueueEntity entity = buildEntity(1L, ConsultationStatus.WAITING.getCode(), 200L);
        ConsultationQueueResponse response = buildResponse(1L);
        when(queueRepository.findByDoctorIdAndStatusOrderByRegisteredAtAsc(200L, "WAITING"))
                .thenReturn(List.of(entity));
        when(converter.toResponse(entity)).thenReturn(response);

        Result<List<ConsultationQueueResponse>> result = service.listWaiting(200L);

        assertEquals("SUCCESS", result.getCode());
        assertEquals(1, result.getData().size());
    }

    @Test
    void callNext_shouldReturnNotFoundWhenNoWaiting() {
        when(queueRepository.findByDoctorIdAndStatusOrderByRegisteredAtAsc(200L, "WAITING"))
                .thenReturn(List.of());

        Result<ConsultationQueueResponse> result = service.callNext(200L);

        assertEquals("NOT_FOUND", result.getCode());
    }

    @Test
    void callNext_shouldCallFirstWaitingAndSetCalledStatus() {
        ConsultationQueueEntity entity = buildEntity(1L, ConsultationStatus.WAITING.getCode(), 200L);
        ConsultationQueueResponse response = buildResponse(1L);
        when(queueRepository.findByDoctorIdAndStatusOrderByRegisteredAtAsc(200L, "WAITING"))
                .thenReturn(List.of(entity));
        when(queueRepository.save(entity)).thenReturn(entity);
        when(converter.toResponse(entity)).thenReturn(response);

        Result<ConsultationQueueResponse> result = service.callNext(200L);

        assertEquals("SUCCESS", result.getCode());
        assertEquals(ConsultationStatus.CALLED.getCode(), entity.getStatus());
        assertNotNull(entity.getCalledAt());
        verify(queueRepository).save(entity);
    }

    @Test
    void startConsultation_shouldReturnNotFoundWhenNotExists() {
        when(queueRepository.findById(1L)).thenReturn(Optional.empty());

        Result<ConsultationQueueResponse> result = service.startConsultation(1L, 200L);

        assertEquals(GlobalErrorCode.CONSULTATION_NOT_FOUND.getCode(), result.getCode());
    }

    @Test
    void startConsultation_shouldReturnForbiddenWhenNotOwner() {
        ConsultationQueueEntity entity = buildEntity(1L, ConsultationStatus.CALLED.getCode(), 999L);
        when(queueRepository.findById(1L)).thenReturn(Optional.of(entity));

        Result<ConsultationQueueResponse> result = service.startConsultation(1L, 200L);

        assertEquals(GlobalErrorCode.FORBIDDEN.getCode(), result.getCode());
    }

    @Test
    void startConsultation_shouldReturnNotCallableWhenStatusNotCalled() {
        ConsultationQueueEntity entity = buildEntity(1L, ConsultationStatus.WAITING.getCode(), 200L);
        when(queueRepository.findById(1L)).thenReturn(Optional.of(entity));

        Result<ConsultationQueueResponse> result = service.startConsultation(1L, 200L);

        assertEquals(GlobalErrorCode.CONSULTATION_NOT_CALLABLE.getCode(), result.getCode());
    }

    @Test
    void startConsultation_shouldSetInConsultationStatus() {
        ConsultationQueueEntity entity = buildEntity(1L, ConsultationStatus.CALLED.getCode(), 200L);
        ConsultationQueueResponse response = buildResponse(1L);
        when(queueRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(queueRepository.save(entity)).thenReturn(entity);
        when(converter.toResponse(entity)).thenReturn(response);

        Result<ConsultationQueueResponse> result = service.startConsultation(1L, 200L);

        assertEquals("SUCCESS", result.getCode());
        assertEquals(ConsultationStatus.IN_CONSULTATION.getCode(), entity.getStatus());
    }

    @Test
    void finishConsultation_shouldReturnNotCallableWhenStatusNotInConsultation() {
        ConsultationQueueEntity entity = buildEntity(1L, ConsultationStatus.CALLED.getCode(), 200L);
        when(queueRepository.findById(1L)).thenReturn(Optional.of(entity));

        Result<ConsultationQueueResponse> result = service.finishConsultation(1L, 200L);

        assertEquals(GlobalErrorCode.CONSULTATION_NOT_CALLABLE.getCode(), result.getCode());
    }

    @Test
    void finishConsultation_shouldSetFinishedStatus() {
        ConsultationQueueEntity entity = buildEntity(1L, ConsultationStatus.IN_CONSULTATION.getCode(), 200L);
        ConsultationQueueResponse response = buildResponse(1L);
        when(queueRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(queueRepository.save(entity)).thenReturn(entity);
        when(converter.toResponse(entity)).thenReturn(response);

        Result<ConsultationQueueResponse> result = service.finishConsultation(1L, 200L);

        assertEquals("SUCCESS", result.getCode());
        assertEquals(ConsultationStatus.FINISHED.getCode(), entity.getStatus());
        assertNotNull(entity.getFinishedAt());
    }

    @Test
    void skip_shouldReturnNotCallableWhenStatusIsInConsultation() {
        ConsultationQueueEntity entity = buildEntity(1L, ConsultationStatus.IN_CONSULTATION.getCode(), 200L);
        when(queueRepository.findById(1L)).thenReturn(Optional.of(entity));

        Result<ConsultationQueueResponse> result = service.skip(1L, 200L);

        assertEquals(GlobalErrorCode.CONSULTATION_NOT_CALLABLE.getCode(), result.getCode());
    }

    @Test
    void skip_shouldSetSkippedStatusFromWaiting() {
        ConsultationQueueEntity entity = buildEntity(1L, ConsultationStatus.WAITING.getCode(), 200L);
        ConsultationQueueResponse response = buildResponse(1L);
        when(queueRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(queueRepository.save(entity)).thenReturn(entity);
        when(converter.toResponse(entity)).thenReturn(response);

        Result<ConsultationQueueResponse> result = service.skip(1L, 200L);

        assertEquals("SUCCESS", result.getCode());
        assertEquals(ConsultationStatus.SKIPPED.getCode(), entity.getStatus());
    }

    @Test
    void skip_shouldSetSkippedStatusFromCalled() {
        ConsultationQueueEntity entity = buildEntity(1L, ConsultationStatus.CALLED.getCode(), 200L);
        ConsultationQueueResponse response = buildResponse(1L);
        when(queueRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(queueRepository.save(entity)).thenReturn(entity);
        when(converter.toResponse(entity)).thenReturn(response);

        Result<ConsultationQueueResponse> result = service.skip(1L, 200L);

        assertEquals("SUCCESS", result.getCode());
        assertEquals(ConsultationStatus.SKIPPED.getCode(), entity.getStatus());
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }

    private static <T> List<T> anyList() {
        return org.mockito.ArgumentMatchers.anyList();
    }
}
