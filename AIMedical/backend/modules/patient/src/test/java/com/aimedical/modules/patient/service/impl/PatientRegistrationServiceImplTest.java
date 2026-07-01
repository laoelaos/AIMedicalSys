package com.aimedical.modules.patient.service.impl;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.modules.patient.dto.CancelResponse;
import com.aimedical.modules.patient.dto.RegistrationRequest;
import com.aimedical.modules.patient.dto.RegistrationResponse;
import com.aimedical.modules.patient.entity.RegistrationEntity;
import com.aimedical.modules.patient.repository.PatientRegistrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientRegistrationServiceImplTest {

    @Mock private PatientRegistrationRepository repository;
    @InjectMocks private PatientRegistrationServiceImpl service;

    @Test
    void createShouldSaveWithPendingStatus() {
        when(repository.existsByUserIdAndTimeSlotAndStatusNotAndDeletedFalse(anyLong(), anyString(), anyString()))
                .thenReturn(false);
        RegistrationEntity saved = new RegistrationEntity();
        saved.setId(1L);
        saved.setUserId(3L);
        saved.setRegistrationType("OUTPATIENT");
        saved.setDoctorName("王主任");
        saved.setDoctorId(101L);
        saved.setDepartmentName("神经内科");
        saved.setTimeSlot("07-01 08:00-08:30");
        saved.setStatus("PENDING");
        saved.setCreatedAt(LocalDateTime.now());
        when(repository.save(any(RegistrationEntity.class))).thenReturn(saved);

        RegistrationRequest req = new RegistrationRequest();
        req.setRegistrationType("OUTPATIENT");
        req.setDoctorName("王主任");
        req.setDoctorId(101L);
        req.setDepartmentName("神经内科");
        req.setTimeSlot("07-01 08:00-08:30");

        RegistrationResponse resp = service.create(req, 3L);
        assertEquals("PENDING", saved.getStatus());
        assertNotNull(resp);
        assertEquals("神经内科", resp.getDepartmentName());
    }

    @Test
    void createShouldRejectDuplicate() {
        when(repository.existsByUserIdAndTimeSlotAndStatusNotAndDeletedFalse(3L, "07-01 08:00-08:30", "CANCELLED"))
                .thenReturn(true);

        RegistrationRequest req = new RegistrationRequest();
        req.setRegistrationType("OUTPATIENT");
        req.setDoctorName("王主任");
        req.setTimeSlot("07-01 08:00-08:30");

        assertThrows(BusinessException.class, () -> service.create(req, 3L));
    }

    @Test
    void listByUserShouldReturnList() {
        RegistrationEntity e = new RegistrationEntity();
        e.setId(1L);
        e.setUserId(3L);
        e.setRegistrationType("OUTPATIENT");
        e.setDoctorName("王主任");
        e.setDepartmentName("神经内科");
        e.setTimeSlot("07-01 08:00-08:30");
        e.setStatus("PENDING");
        e.setCreatedAt(LocalDateTime.now());
        when(repository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(3L)).thenReturn(List.of(e));

        List<RegistrationResponse> list = service.listByUser(3L);
        assertEquals(1, list.size());
        assertEquals("神经内科", list.get(0).getDepartmentName());
        assertTrue(list.get(0).isCanCancel());
    }

    @Test
    void cancelShouldSucceedForPendingRegistration() {
        RegistrationEntity e = new RegistrationEntity();
        e.setId(1L);
        e.setUserId(3L);
        e.setRegistrationType("OUTPATIENT");
        e.setTimeSlot("07-10 08:00-08:30"); // Far future
        e.setStatus("PENDING");
        when(repository.findById(1L)).thenReturn(Optional.of(e));

        CancelResponse resp = service.cancel(1L, 3L);
        assertTrue(resp.isSuccess());
        assertEquals("挂号已成功取消", resp.getMessage());
        verify(repository).save(e);
    }

    @Test
    void cancelShouldRejectForConfirmed() {
        RegistrationEntity e = new RegistrationEntity();
        e.setId(1L);
        e.setUserId(3L);
        e.setRegistrationType("OUTPATIENT");
        e.setTimeSlot("07-10 08:00-08:30");
        e.setStatus("CONFIRMED");
        when(repository.findById(1L)).thenReturn(Optional.of(e));

        CancelResponse resp = service.cancel(1L, 3L);
        assertFalse(resp.isSuccess());
        assertEquals("已确认挂号不支持在线取消，请联系线下窗口", resp.getMessage());
    }

    @Test
    void cancelShouldRejectForDispensed() {
        RegistrationEntity e = new RegistrationEntity();
        e.setId(1L);
        e.setUserId(3L);
        e.setRegistrationType("OUTPATIENT");
        e.setTimeSlot("07-10 08:00-08:30");
        e.setStatus("DISPENSED");
        when(repository.findById(1L)).thenReturn(Optional.of(e));

        CancelResponse resp = service.cancel(1L, 3L);
        assertFalse(resp.isSuccess());
        assertEquals("已发药处方请携带至线下窗口办理退费", resp.getMessage());
    }

    @Test
    void cancelShouldRejectForOtherUser() {
        RegistrationEntity e = new RegistrationEntity();
        e.setId(1L);
        e.setUserId(5L);
        e.setStatus("PENDING");
        when(repository.findById(1L)).thenReturn(Optional.of(e));

        assertThrows(BusinessException.class, () -> service.cancel(1L, 3L));
    }

    @Test
    void toResponseShouldMarkConfirmAsNotCancellable() {
        RegistrationEntity e = new RegistrationEntity();
        e.setId(1L);
        e.setUserId(3L);
        e.setRegistrationType("OUTPATIENT");
        e.setDoctorName("王主任");
        e.setDepartmentName("神经内科");
        e.setTimeSlot("07-01 08:00-08:30");
        e.setStatus("CONFIRMED");
        e.setCreatedAt(LocalDateTime.now());
        when(repository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(3L)).thenReturn(List.of(e));

        List<RegistrationResponse> list = service.listByUser(3L);
        assertEquals(1, list.size());
        assertFalse(list.get(0).isCanCancel());
    }

    @Test
    void createShouldHandleNullTimeSlot() {
        when(repository.save(any(RegistrationEntity.class))).thenAnswer(inv -> {
            RegistrationEntity e = inv.getArgument(0);
            e.setId(1L);
            e.setCreatedAt(LocalDateTime.now());
            return e;
        });
        RegistrationRequest req = new RegistrationRequest();
        req.setRegistrationType("OUTPATIENT");
        req.setDoctorName("王主任");
        // timeSlot is null

        RegistrationResponse resp = service.create(req, 3L);
        assertNotNull(resp);
    }

    @Test
    void cancelShouldHandleAlreadyCancelled() {
        RegistrationEntity e = new RegistrationEntity();
        e.setId(1L);
        e.setUserId(3L);
        e.setTimeSlot("07-10 08:00-08:30");
        e.setStatus("CANCELLED");
        when(repository.findById(1L)).thenReturn(Optional.of(e));

        CancelResponse resp = service.cancel(1L, 3L);
        assertFalse(resp.isSuccess());
        assertEquals("该挂号已取消", resp.getMessage());
    }

    @Test
    void cancelShouldThrowForMissingEntity() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.cancel(99L, 3L));
    }

    @Test
    void createOutpatientWithoutDoctorShouldFail() {
        RegistrationRequest req = new RegistrationRequest();
        req.setRegistrationType("OUTPATIENT");
        req.setTimeSlot("07-10 08:00-08:30");
        assertThrows(BusinessException.class, () -> service.create(req, 3L));
    }

    @Test
    void createExaminationWithoutItemShouldFail() {
        RegistrationRequest req = new RegistrationRequest();
        req.setRegistrationType("EXAMINATION");
        req.setTimeSlot("07-10 08:00-08:30");
        assertThrows(BusinessException.class, () -> service.create(req, 3L));
    }

    @Test
    void cancelShouldRejectFinished() {
        RegistrationEntity e = new RegistrationEntity();
        e.setId(1L);
        e.setUserId(3L);
        e.setRegistrationType("OUTPATIENT");
        e.setTimeSlot("07-10 08:00-08:30");
        e.setStatus("FINISHED");
        when(repository.findById(1L)).thenReturn(Optional.of(e));

        CancelResponse resp = service.cancel(1L, 3L);
        assertFalse(resp.isSuccess());
        assertEquals("该挂号已完成就诊，不可取消", resp.getMessage());
    }
}
