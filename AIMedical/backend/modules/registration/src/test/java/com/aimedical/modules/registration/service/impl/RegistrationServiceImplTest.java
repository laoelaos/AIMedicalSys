package com.aimedical.modules.registration.service.impl;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.modules.doctor.service.DoctorService;
import com.aimedical.modules.patient.service.PatientService;
import com.aimedical.modules.registration.dto.CancelRegistrationRequest;
import com.aimedical.modules.registration.dto.RegistrationDTO;
import com.aimedical.modules.registration.dto.TriageRecordDTO;
import com.aimedical.modules.registration.entity.Registration;
import com.aimedical.modules.registration.entity.RegistrationStatus;
import com.aimedical.modules.registration.entity.RegistrationType;
import com.aimedical.modules.registration.entity.TriageLevel;
import com.aimedical.modules.registration.entity.TriageRecord;
import com.aimedical.modules.registration.repository.RegistrationRepository;
import com.aimedical.modules.registration.repository.TriageRecordRepository;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RegistrationServiceImpl 单元测试。
 * 覆盖 createRegistration / getRegistration / confirmRegistration / completeRegistration /
 * cancelRegistration / markNoShow / createTriageRecord / getTriageRecord 等方法的所有关键分支。
 */
@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private TriageRecordRepository triageRecordRepository;

    @Mock
    private PatientService patientService;

    @Mock
    private DoctorService doctorService;

    @InjectMocks
    private RegistrationServiceImpl service;

    private RegistrationDTO baseDto() {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setPatientId(1L);
        dto.setDoctorId(10L);
        dto.setRegistrationType(RegistrationType.OUTPATIENT);
        dto.setDepartment("内科");
        dto.setScheduledDate(LocalDate.now().plusDays(1));
        dto.setScheduledTimeSlot("09:00-10:00");
        return dto;
    }

    private Registration registration(Long id, RegistrationStatus status) {
        Registration entity = new Registration();
        entity.setId(id);
        entity.setStatus(status);
        entity.setPatientId(1L);
        entity.setDoctorId(10L);
        entity.setScheduledDate(LocalDate.now().plusDays(1));
        entity.setScheduledTimeSlot("09:00-10:00");
        return entity;
    }

    @Nested
    @DisplayName("createRegistration")
    class CreateRegistration {

        @Test
        @DisplayName("门诊挂号成功（带科室）")
        void outpatientWithDepartment() {
            RegistrationDTO dto = baseDto();
            when(patientService.existsById(1L)).thenReturn(true);
            when(doctorService.existsById(10L)).thenReturn(true);
            when(registrationRepository.countByScheduledDateAndDoctorId(dto.getScheduledDate(), 10L))
                    .thenReturn(2L);
            when(registrationRepository.save(any(Registration.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            RegistrationDTO result = service.createRegistration(dto);

            assertNotNull(result);
            assertEquals(RegistrationStatus.PENDING, result.getStatus());
            assertEquals(3, result.getQueueNumber());
            assertEquals("内科", result.getDepartment());
        }

        @Test
        @DisplayName("门诊挂号成功（无科室）")
        void outpatientWithoutDepartment() {
            RegistrationDTO dto = baseDto();
            dto.setDepartment(null);
            when(patientService.existsById(1L)).thenReturn(true);
            when(doctorService.existsById(10L)).thenReturn(true);
            when(registrationRepository.countByScheduledDateAndDoctorId(any(), any()))
                    .thenReturn(0L);
            when(registrationRepository.save(any(Registration.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            RegistrationDTO result = service.createRegistration(dto);

            assertNotNull(result);
            assertEquals(1, result.getQueueNumber());
            assertNull(result.getDepartment());
        }

        @Test
        @DisplayName("检查挂号成功（带科室）")
        void examinationWithDepartment() {
            RegistrationDTO dto = baseDto();
            dto.setRegistrationType(RegistrationType.EXAMINATION);
            dto.setDepartment("检验科");
            when(patientService.existsById(1L)).thenReturn(true);
            when(doctorService.existsById(10L)).thenReturn(true);
            when(registrationRepository.countByScheduledDateAndDoctorId(any(), any()))
                    .thenReturn(0L);
            when(registrationRepository.save(any(Registration.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            RegistrationDTO result = service.createRegistration(dto);

            assertEquals("检验科", result.getDepartment());
        }

        @Test
        @DisplayName("检查挂号无科室抛参数异常")
        void examinationWithoutDepartmentThrows() {
            RegistrationDTO dto = baseDto();
            dto.setRegistrationType(RegistrationType.EXAMINATION);
            dto.setDepartment(null);
            when(patientService.existsById(1L)).thenReturn(true);
            when(doctorService.existsById(10L)).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createRegistration(dto));
            assertEquals(GlobalErrorCode.PARAM_INVALID, ex.getErrorCode());
        }

        @Test
        @DisplayName("急诊挂号设置一级分诊")
        void emergencySetsTriageLevel() {
            RegistrationDTO dto = baseDto();
            dto.setRegistrationType(RegistrationType.EMERGENCY);
            when(patientService.existsById(1L)).thenReturn(true);
            when(doctorService.existsById(10L)).thenReturn(true);
            when(registrationRepository.countByScheduledDateAndDoctorId(any(), any()))
                    .thenReturn(0L);
            when(registrationRepository.save(any(Registration.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            RegistrationDTO result = service.createRegistration(dto);

            assertEquals(TriageLevel.LEVEL_1, result.getTriageLevel());
        }

        @Test
        @DisplayName("患者不存在抛 NOT_FOUND")
        void patientNotFound() {
            RegistrationDTO dto = baseDto();
            when(patientService.existsById(1L)).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createRegistration(dto));
            assertEquals(GlobalErrorCode.NOT_FOUND, ex.getErrorCode());
            verify(doctorService, never()).existsById(anyLong());
        }

        @Test
        @DisplayName("医生不存在抛 NOT_FOUND")
        void doctorNotFound() {
            RegistrationDTO dto = baseDto();
            when(patientService.existsById(1L)).thenReturn(true);
            when(doctorService.existsById(10L)).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createRegistration(dto));
            assertEquals(GlobalErrorCode.NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("患者ID为空时跳过患者校验")
        void nullPatientIdSkipsCheck() {
            RegistrationDTO dto = baseDto();
            dto.setPatientId(null);
            when(doctorService.existsById(10L)).thenReturn(true);
            when(registrationRepository.countByScheduledDateAndDoctorId(any(), any()))
                    .thenReturn(0L);
            when(registrationRepository.save(any(Registration.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            RegistrationDTO result = service.createRegistration(dto);

            assertNotNull(result);
            verify(patientService, never()).existsById(anyLong());
        }

        @Test
        @DisplayName("挂号类型为空时跳过类型分支")
        void nullRegistrationTypeSkipsSwitch() {
            RegistrationDTO dto = baseDto();
            dto.setRegistrationType(null);
            when(patientService.existsById(1L)).thenReturn(true);
            when(doctorService.existsById(10L)).thenReturn(true);
            when(registrationRepository.countByScheduledDateAndDoctorId(any(), any()))
                    .thenReturn(0L);
            when(registrationRepository.save(any(Registration.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            RegistrationDTO result = service.createRegistration(dto);

            assertNotNull(result);
            assertNull(result.getRegistrationType());
        }

        @Test
        @DisplayName("医生ID和预约日期为空抛参数异常")
        void nullDoctorAndDateThrows() {
            RegistrationDTO dto = baseDto();
            dto.setDoctorId(null);
            dto.setScheduledDate(null);
            when(patientService.existsById(1L)).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createRegistration(dto));
            assertEquals(GlobalErrorCode.PARAM_INVALID, ex.getErrorCode());
        }

        @Test
        @DisplayName("排号冲突重试一次后成功")
        void saveRetrySucceedsAfterConflict() {
            RegistrationDTO dto = baseDto();
            when(patientService.existsById(1L)).thenReturn(true);
            when(doctorService.existsById(10L)).thenReturn(true);
            when(registrationRepository.countByScheduledDateAndDoctorId(any(), any()))
                    .thenReturn(0L);
            when(registrationRepository.save(any(Registration.class)))
                    .thenThrow(new DataIntegrityViolationException("conflict"))
                    .thenAnswer(inv -> inv.getArgument(0));

            RegistrationDTO result = service.createRegistration(dto);

            assertNotNull(result);
            verify(registrationRepository, times(2)).save(any(Registration.class));
        }

        @Test
        @DisplayName("排号冲突重试耗尽抛 SYSTEM_ERROR")
        void saveRetryExhausted() {
            RegistrationDTO dto = baseDto();
            when(patientService.existsById(1L)).thenReturn(true);
            when(doctorService.existsById(10L)).thenReturn(true);
            when(registrationRepository.countByScheduledDateAndDoctorId(any(), any()))
                    .thenReturn(0L);
            when(registrationRepository.save(any(Registration.class)))
                    .thenThrow(new DataIntegrityViolationException("conflict"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createRegistration(dto));
            assertEquals(GlobalErrorCode.SYSTEM_ERROR, ex.getErrorCode());
            verify(registrationRepository, times(3)).save(any(Registration.class));
        }
    }

    @Nested
    @DisplayName("getRegistration")
    class GetRegistration {

        @Test
        @DisplayName("找到返回DTO")
        void found() {
            Registration entity = registration(1L, RegistrationStatus.PENDING);
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(entity));

            RegistrationDTO result = service.getRegistration(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("未找到抛 NOT_FOUND")
        void notFound() {
            when(registrationRepository.findById(1L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.getRegistration(1L));
            assertEquals(GlobalErrorCode.NOT_FOUND, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("confirmRegistration")
    class ConfirmRegistration {

        @Test
        @DisplayName("PENDING 状态确认成功")
        void pendingConfirmSuccess() {
            Registration entity = registration(1L, RegistrationStatus.PENDING);
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(registrationRepository.save(any(Registration.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            RegistrationDTO result = service.confirmRegistration(1L);

            assertEquals(RegistrationStatus.CONFIRMED, result.getStatus());
        }

        @Test
        @DisplayName("非 PENDING 状态抛 REGISTRATION_STATUS_INVALID")
        void nonPendingThrows() {
            Registration entity = registration(1L, RegistrationStatus.CONFIRMED);
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(entity));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.confirmRegistration(1L));
            assertEquals(GlobalErrorCode.REGISTRATION_STATUS_INVALID, ex.getErrorCode());
        }

        @Test
        @DisplayName("挂号不存在抛 NOT_FOUND")
        void notFound() {
            when(registrationRepository.findById(1L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.confirmRegistration(1L));
            assertEquals(GlobalErrorCode.NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("乐观锁冲突抛 SYSTEM_ERROR")
        void optimisticLockThrows() {
            Registration entity = registration(1L, RegistrationStatus.PENDING);
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(registrationRepository.save(any(Registration.class)))
                    .thenThrow(new OptimisticLockException("conflict"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.confirmRegistration(1L));
            assertEquals(GlobalErrorCode.SYSTEM_ERROR, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("completeRegistration")
    class CompleteRegistration {

        @Test
        @DisplayName("CONFIRMED 状态完成成功")
        void confirmedCompleteSuccess() {
            Registration entity = registration(1L, RegistrationStatus.CONFIRMED);
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(registrationRepository.save(any(Registration.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            RegistrationDTO result = service.completeRegistration(1L);

            assertEquals(RegistrationStatus.COMPLETED, result.getStatus());
        }

        @Test
        @DisplayName("非 CONFIRMED 状态抛 REGISTRATION_STATUS_INVALID")
        void nonConfirmedThrows() {
            Registration entity = registration(1L, RegistrationStatus.PENDING);
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(entity));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.completeRegistration(1L));
            assertEquals(GlobalErrorCode.REGISTRATION_STATUS_INVALID, ex.getErrorCode());
        }

        @Test
        @DisplayName("挂号不存在抛 NOT_FOUND")
        void notFound() {
            when(registrationRepository.findById(1L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.completeRegistration(1L));
            assertEquals(GlobalErrorCode.NOT_FOUND, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("cancelRegistration")
    class CancelRegistration {

        private CancelRegistrationRequest request() {
            CancelRegistrationRequest req = new CancelRegistrationRequest();
            req.setCancelReason("计划变更");
            return req;
        }

        @Test
        @DisplayName("PENDING + 距今大于2小时(HH:mm-HH:mm格式) → online 取消成功")
        void pendingOnlineCancelSuccess() {
            Registration entity = registration(1L, RegistrationStatus.PENDING);
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(registrationRepository.save(any(Registration.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            RegistrationDTO result = service.cancelRegistration(1L, request());

            assertEquals(RegistrationStatus.CANCELLED, result.getStatus());
            assertEquals("online", result.getCancelType());
            assertEquals("计划变更", result.getCancelReason());
            assertNotNull(result.getCancelTime());
        }

        @Test
        @DisplayName("CONFIRMED + 距今大于2小时 → online 取消成功")
        void confirmedOnlineCancelSuccess() {
            Registration entity = registration(1L, RegistrationStatus.CONFIRMED);
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(registrationRepository.save(any(Registration.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            RegistrationDTO result = service.cancelRegistration(1L, request());

            assertEquals(RegistrationStatus.CANCELLED, result.getStatus());
            assertEquals("online", result.getCancelType());
        }

        @Test
        @DisplayName("PENDING + AM 时段(明天) → online 取消成功")
        void pendingAmSlotOnlineCancel() {
            Registration entity = registration(1L, RegistrationStatus.PENDING);
            entity.setScheduledTimeSlot("AM");
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(registrationRepository.save(any(Registration.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            RegistrationDTO result = service.cancelRegistration(1L, request());

            assertEquals("online", result.getCancelType());
        }

        @Test
        @DisplayName("PENDING + PM 时段(明天) → online 取消成功")
        void pendingPmSlotOnlineCancel() {
            Registration entity = registration(1L, RegistrationStatus.PENDING);
            entity.setScheduledTimeSlot("PM");
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(registrationRepository.save(any(Registration.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            RegistrationDTO result = service.cancelRegistration(1L, request());

            assertEquals("online", result.getCancelType());
        }

        @Test
        @DisplayName("PENDING + 今天已过去时段 → offline 抛 REGISTRATION_CANCEL_FORBIDDEN")
        void pendingOfflineTodayThrows() {
            Registration entity = registration(1L, RegistrationStatus.PENDING);
            entity.setScheduledDate(LocalDate.now());
            entity.setScheduledTimeSlot("00:00-01:00");
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(entity));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.cancelRegistration(1L, request()));
            assertEquals(GlobalErrorCode.REGISTRATION_CANCEL_FORBIDDEN, ex.getErrorCode());
        }

        @Test
        @DisplayName("PENDING + scheduledDate 为空 → offline 抛 REGISTRATION_CANCEL_FORBIDDEN")
        void pendingNullDateOfflineThrows() {
            Registration entity = registration(1L, RegistrationStatus.PENDING);
            entity.setScheduledDate(null);
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(entity));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.cancelRegistration(1L, request()));
            assertEquals(GlobalErrorCode.REGISTRATION_CANCEL_FORBIDDEN, ex.getErrorCode());
        }

        @Test
        @DisplayName("PENDING + scheduledTimeSlot 为空 → offline 抛 REGISTRATION_CANCEL_FORBIDDEN")
        void pendingNullSlotOfflineThrows() {
            Registration entity = registration(1L, RegistrationStatus.PENDING);
            entity.setScheduledTimeSlot(null);
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(entity));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.cancelRegistration(1L, request()));
            assertEquals(GlobalErrorCode.REGISTRATION_CANCEL_FORBIDDEN, ex.getErrorCode());
        }

        @Test
        @DisplayName("PENDING + 非法时段格式 → offline 抛 REGISTRATION_CANCEL_FORBIDDEN")
        void pendingInvalidSlotOfflineThrows() {
            Registration entity = registration(1L, RegistrationStatus.PENDING);
            entity.setScheduledTimeSlot("invalid");
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(entity));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.cancelRegistration(1L, request()));
            assertEquals(GlobalErrorCode.REGISTRATION_CANCEL_FORBIDDEN, ex.getErrorCode());
        }

        @Test
        @DisplayName("COMPLETED 状态抛 REGISTRATION_STATUS_INVALID")
        void completedThrows() {
            Registration entity = registration(1L, RegistrationStatus.COMPLETED);
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(entity));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.cancelRegistration(1L, request()));
            assertEquals(GlobalErrorCode.REGISTRATION_STATUS_INVALID, ex.getErrorCode());
        }

        @Test
        @DisplayName("CANCELLED 状态抛 REGISTRATION_STATUS_INVALID")
        void cancelledThrows() {
            Registration entity = registration(1L, RegistrationStatus.CANCELLED);
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(entity));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.cancelRegistration(1L, request()));
            assertEquals(GlobalErrorCode.REGISTRATION_STATUS_INVALID, ex.getErrorCode());
        }

        @Test
        @DisplayName("NO_SHOW 状态抛 REGISTRATION_STATUS_INVALID")
        void noShowThrows() {
            Registration entity = registration(1L, RegistrationStatus.NO_SHOW);
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(entity));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.cancelRegistration(1L, request()));
            assertEquals(GlobalErrorCode.REGISTRATION_STATUS_INVALID, ex.getErrorCode());
        }

        @Test
        @DisplayName("挂号不存在抛 NOT_FOUND")
        void notFound() {
            when(registrationRepository.findById(1L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.cancelRegistration(1L, request()));
            assertEquals(GlobalErrorCode.NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("保存时乐观锁冲突抛 SYSTEM_ERROR")
        void optimisticLockThrows() {
            Registration entity = registration(1L, RegistrationStatus.PENDING);
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(registrationRepository.save(any(Registration.class)))
                    .thenThrow(new OptimisticLockException("conflict"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.cancelRegistration(1L, request()));
            assertEquals(GlobalErrorCode.SYSTEM_ERROR, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("markNoShow")
    class MarkNoShow {

        @Test
        @DisplayName("CONFIRMED 状态标记未到诊成功")
        void confirmedMarkNoShowSuccess() {
            Registration entity = registration(1L, RegistrationStatus.CONFIRMED);
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(registrationRepository.save(any(Registration.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            RegistrationDTO result = service.markNoShow(1L);

            assertEquals(RegistrationStatus.NO_SHOW, result.getStatus());
        }

        @Test
        @DisplayName("非 CONFIRMED 状态抛 REGISTRATION_STATUS_INVALID")
        void nonConfirmedThrows() {
            Registration entity = registration(1L, RegistrationStatus.PENDING);
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(entity));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.markNoShow(1L));
            assertEquals(GlobalErrorCode.REGISTRATION_STATUS_INVALID, ex.getErrorCode());
        }

        @Test
        @DisplayName("挂号不存在抛 NOT_FOUND")
        void notFound() {
            when(registrationRepository.findById(1L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.markNoShow(1L));
            assertEquals(GlobalErrorCode.NOT_FOUND, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("createTriageRecord")
    class CreateTriageRecord {

        private TriageRecordDTO baseTriageDto() {
            TriageRecordDTO dto = new TriageRecordDTO();
            dto.setRegistrationId(1L);
            dto.setPatientId(1L);
            dto.setNurseId(100L);
            dto.setSymptoms("头痛");
            dto.setTriageLevel(TriageLevel.LEVEL_3);
            dto.setTriageDepartment("急诊科");
            return dto;
        }

        @Test
        @DisplayName("PENDING 挂号分诊成功")
        void pendingTriageSuccess() {
            TriageRecordDTO dto = baseTriageDto();
            Registration reg = registration(1L, RegistrationStatus.PENDING);
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
            when(triageRecordRepository.findByRegistrationId(1L)).thenReturn(Optional.empty());
            when(triageRecordRepository.save(any(TriageRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            TriageRecordDTO result = service.createTriageRecord(dto);

            assertNotNull(result);
            assertEquals(1L, result.getRegistrationId());
            assertEquals(TriageLevel.LEVEL_3, result.getTriageLevel());
        }

        @Test
        @DisplayName("CONFIRMED 挂号分诊成功")
        void confirmedTriageSuccess() {
            TriageRecordDTO dto = baseTriageDto();
            Registration reg = registration(1L, RegistrationStatus.CONFIRMED);
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
            when(triageRecordRepository.findByRegistrationId(1L)).thenReturn(Optional.empty());
            when(triageRecordRepository.save(any(TriageRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            TriageRecordDTO result = service.createTriageRecord(dto);

            assertNotNull(result);
        }

        @Test
        @DisplayName("挂号记录不存在抛 NOT_FOUND")
        void registrationNotFound() {
            TriageRecordDTO dto = baseTriageDto();
            when(registrationRepository.findById(1L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createTriageRecord(dto));
            assertEquals(GlobalErrorCode.NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("CANCELLED 状态不允许分诊")
        void cancelledStatusThrows() {
            TriageRecordDTO dto = baseTriageDto();
            Registration reg = registration(1L, RegistrationStatus.CANCELLED);
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createTriageRecord(dto));
            assertEquals(GlobalErrorCode.REGISTRATION_STATUS_INVALID, ex.getErrorCode());
        }

        @Test
        @DisplayName("NO_SHOW 状态不允许分诊")
        void noShowStatusThrows() {
            TriageRecordDTO dto = baseTriageDto();
            Registration reg = registration(1L, RegistrationStatus.NO_SHOW);
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createTriageRecord(dto));
            assertEquals(GlobalErrorCode.REGISTRATION_STATUS_INVALID, ex.getErrorCode());
        }

        @Test
        @DisplayName("COMPLETED 状态不允许分诊")
        void completedStatusThrows() {
            TriageRecordDTO dto = baseTriageDto();
            Registration reg = registration(1L, RegistrationStatus.COMPLETED);
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createTriageRecord(dto));
            assertEquals(GlobalErrorCode.REGISTRATION_STATUS_INVALID, ex.getErrorCode());
        }

        @Test
        @DisplayName("重复分诊抛 TRIAGE_RECORD_EXISTS")
        void duplicateThrows() {
            TriageRecordDTO dto = baseTriageDto();
            Registration reg = registration(1L, RegistrationStatus.PENDING);
            TriageRecord existing = new TriageRecord();
            existing.setId(99L);
            when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));
            when(triageRecordRepository.findByRegistrationId(1L)).thenReturn(Optional.of(existing));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createTriageRecord(dto));
            assertEquals(GlobalErrorCode.TRIAGE_RECORD_EXISTS, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("getTriageRecord")
    class GetTriageRecord {

        @Test
        @DisplayName("找到返回 DTO")
        void found() {
            TriageRecord entity = new TriageRecord();
            entity.setId(1L);
            entity.setRegistrationId(1L);
            entity.setTriageLevel(TriageLevel.LEVEL_2);
            when(triageRecordRepository.findByRegistrationId(1L)).thenReturn(Optional.of(entity));

            TriageRecordDTO result = service.getTriageRecord(1L);

            assertNotNull(result);
            assertEquals(1L, result.getRegistrationId());
            assertEquals(TriageLevel.LEVEL_2, result.getTriageLevel());
        }

        @Test
        @DisplayName("未找到抛 NOT_FOUND")
        void notFound() {
            when(triageRecordRepository.findByRegistrationId(1L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.getTriageRecord(1L));
            assertEquals(GlobalErrorCode.NOT_FOUND, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("getRegistrationsByPatient")
    class GetRegistrationsByPatient {

        @Test
        @DisplayName("返回患者挂号分页")
        void shouldReturnPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Registration entity = registration(1L, RegistrationStatus.PENDING);
            Page<Registration> page = new PageImpl<>(List.of(entity));
            when(registrationRepository.findByPatientId(1L, pageable)).thenReturn(page);

            Page<RegistrationDTO> result = service.getRegistrationsByPatient(1L, pageable);

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals(1L, result.getContent().get(0).getId());
        }

        @Test
        @DisplayName("空页")
        void shouldReturnEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Registration> emptyPage = new PageImpl<>(List.of());
            when(registrationRepository.findByPatientId(1L, pageable)).thenReturn(emptyPage);

            Page<RegistrationDTO> result = service.getRegistrationsByPatient(1L, pageable);

            assertNotNull(result);
            assertTrue(result.getContent().isEmpty());
        }
    }

    @Nested
    @DisplayName("getRegistrationsByDoctor")
    class GetRegistrationsByDoctor {

        @Test
        @DisplayName("返回医生挂号分页")
        void shouldReturnPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Registration entity = registration(1L, RegistrationStatus.CONFIRMED);
            Page<Registration> page = new PageImpl<>(List.of(entity));
            when(registrationRepository.findByDoctorId(1L, pageable)).thenReturn(page);

            Page<RegistrationDTO> result = service.getRegistrationsByDoctor(1L, pageable);

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
        }

        @Test
        @DisplayName("空页")
        void shouldReturnEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Registration> emptyPage = new PageImpl<>(List.of());
            when(registrationRepository.findByDoctorId(1L, pageable)).thenReturn(emptyPage);

            Page<RegistrationDTO> result = service.getRegistrationsByDoctor(1L, pageable);

            assertNotNull(result);
            assertTrue(result.getContent().isEmpty());
        }
    }
}
