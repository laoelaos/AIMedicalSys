package com.aimedical.modules.registration.service.impl;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.modules.doctor.repository.DoctorRepository;
import com.aimedical.modules.patient.repository.PatientRepository;
import com.aimedical.modules.registration.converter.RegistrationConverter;
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
import com.aimedical.modules.registration.service.RegistrationService;
import jakarta.persistence.OptimisticLockException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final TriageRecordRepository triageRecordRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    /** 排号生成的锁对象，保证同一医生同一日期的排号原子性 */
    private final Object queueLock = new Object();

    public RegistrationServiceImpl(RegistrationRepository registrationRepository,
                                   TriageRecordRepository triageRecordRepository,
                                   PatientRepository patientRepository,
                                   DoctorRepository doctorRepository) {
        this.registrationRepository = registrationRepository;
        this.triageRecordRepository = triageRecordRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    @Override
    @Transactional
    public RegistrationDTO createRegistration(RegistrationDTO dto) {
        // 校验患者和医生是否存在
        if (dto.getPatientId() != null) {
            patientRepository.findById(dto.getPatientId())
                    .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "患者不存在: " + dto.getPatientId()));
        }
        if (dto.getDoctorId() != null) {
            doctorRepository.findById(dto.getDoctorId())
                    .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "医生不存在: " + dto.getDoctorId()));
        }

        Registration entity = RegistrationConverter.toRegistrationEntity(dto);
        entity.setStatus(RegistrationStatus.PENDING);
        entity.setQueueNumber(generateQueueNumber(dto.getDoctorId(), dto.getScheduledDate()));

        // Branch logic based on registration type
        RegistrationType registrationType = dto.getRegistrationType();
        if (registrationType != null) {
            switch (registrationType) {
                case OUTPATIENT:
                    // 门诊挂号：设置默认科室
                    if (dto.getDepartment() != null) {
                        entity.setDepartment(dto.getDepartment());
                    }
                    break;
                case EXAMINATION:
                    // 检查挂号：必须指定科室
                    if (entity.getDepartment() == null) {
                        throw new BusinessException(GlobalErrorCode.PARAM_INVALID, "检查挂号必须指定科室");
                    }
                    break;
                case EMERGENCY:
                    // 急诊挂号：设置为最高分诊级别
                    entity.setTriageLevel(TriageLevel.LEVEL_1);
                    break;
                default:
                    break;
            }
        }

        entity = registrationRepository.save(entity);
        return RegistrationConverter.toRegistrationDTO(entity);
    }

    @Override
    public RegistrationDTO getRegistration(Long id) {
        Registration entity = registrationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND));
        return RegistrationConverter.toRegistrationDTO(entity);
    }

    @Override
    public Page<RegistrationDTO> getRegistrationsByPatient(Long patientId, Pageable pageable) {
        return registrationRepository.findByPatientId(patientId, pageable)
                .map(RegistrationConverter::toRegistrationDTO);
    }

    @Override
    public Page<RegistrationDTO> getRegistrationsByDoctor(Long doctorId, Pageable pageable) {
        return registrationRepository.findByDoctorId(doctorId, pageable)
                .map(RegistrationConverter::toRegistrationDTO);
    }

    @Override
    @Transactional
    public RegistrationDTO confirmRegistration(Long id) {
        Registration entity = registrationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND));

        if (entity.getStatus() != RegistrationStatus.PENDING) {
            throw new BusinessException(GlobalErrorCode.REGISTRATION_STATUS_INVALID);
        }

        entity.setStatus(RegistrationStatus.CONFIRMED);
        entity = saveWithOptimisticLock(entity, "确认挂号");
        return RegistrationConverter.toRegistrationDTO(entity);
    }

    @Override
    @Transactional
    public RegistrationDTO completeRegistration(Long id) {
        Registration entity = registrationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND));

        if (entity.getStatus() != RegistrationStatus.CONFIRMED) {
            throw new BusinessException(GlobalErrorCode.REGISTRATION_STATUS_INVALID);
        }

        entity.setStatus(RegistrationStatus.COMPLETED);
        entity = saveWithOptimisticLock(entity, "完成挂号");
        return RegistrationConverter.toRegistrationDTO(entity);
    }

    @Override
    @Transactional
    public RegistrationDTO cancelRegistration(Long id, CancelRegistrationRequest request) {
        Registration entity = registrationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND));

        if (entity.getStatus() != RegistrationStatus.PENDING
                && entity.getStatus() != RegistrationStatus.CONFIRMED) {
            throw new BusinessException(GlobalErrorCode.REGISTRATION_STATUS_INVALID);
        }

        // Cancel rule engine: check if scheduled time is at least 2 hours away
        String cancelType = determineCancelType(entity.getScheduledDate(), entity.getScheduledTimeSlot());
        if ("offline".equals(cancelType)) {
            throw new BusinessException(GlobalErrorCode.REGISTRATION_CANCEL_FORBIDDEN);
        }

        entity.setStatus(RegistrationStatus.CANCELLED);
        entity.setCancelReason(request.getCancelReason());
        entity.setCancelTime(LocalDateTime.now());
        entity.setCancelType(cancelType);
        entity = saveWithOptimisticLock(entity, "取消挂号");
        return RegistrationConverter.toRegistrationDTO(entity);
    }

    @Override
    @Transactional
    public RegistrationDTO markNoShow(Long id) {
        Registration entity = registrationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND));

        if (entity.getStatus() != RegistrationStatus.CONFIRMED) {
            throw new BusinessException(GlobalErrorCode.REGISTRATION_STATUS_INVALID);
        }

        entity.setStatus(RegistrationStatus.NO_SHOW);
        entity = saveWithOptimisticLock(entity, "标记未到诊");
        return RegistrationConverter.toRegistrationDTO(entity);
    }

    @Override
    @Transactional
    public TriageRecordDTO createTriageRecord(TriageRecordDTO dto) {
        // 校验挂号记录是否存在
        Registration registration = registrationRepository.findById(dto.getRegistrationId())
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND,
                        "挂号记录不存在: " + dto.getRegistrationId()));

        // 校验挂号状态是否允许分诊
        RegistrationStatus status = registration.getStatus();
        if (status == RegistrationStatus.CANCELLED
                || status == RegistrationStatus.NO_SHOW
                || status == RegistrationStatus.COMPLETED) {
            throw new BusinessException(GlobalErrorCode.REGISTRATION_STATUS_INVALID,
                    "当前挂号状态不允许分诊: " + status.getDesc());
        }

        // 重复创建防护
        triageRecordRepository.findByRegistrationId(dto.getRegistrationId()).ifPresent(existing -> {
            throw new BusinessException(GlobalErrorCode.TRIAGE_RECORD_EXISTS,
                    "该挂号已存在分诊记录: " + existing.getId());
        });

        TriageRecord entity = RegistrationConverter.toTriageRecordEntity(dto);
        entity = triageRecordRepository.save(entity);
        return RegistrationConverter.toTriageRecordDTO(entity);
    }

    @Override
    public TriageRecordDTO getTriageRecord(Long registrationId) {
        TriageRecord entity = triageRecordRepository.findByRegistrationId(registrationId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND));
        return RegistrationConverter.toTriageRecordDTO(entity);
    }

    private int generateQueueNumber(Long doctorId, LocalDate scheduledDate) {
        if (scheduledDate == null || doctorId == null) {
            return 1;
        }
        synchronized (queueLock) {
            long count = registrationRepository.countByScheduledDateAndDoctorId(scheduledDate, doctorId);
            return (int) count + 1;
        }
    }

    private String determineCancelType(LocalDate scheduledDate, String scheduledTimeSlot) {
        if (scheduledDate == null || scheduledTimeSlot == null) {
            return "offline";
        }

        try {
            LocalTime startTime = parseTimeSlotStart(scheduledTimeSlot);
            LocalDateTime scheduledDateTime = LocalDateTime.of(scheduledDate, startTime);
            LocalDateTime now = LocalDateTime.now();

            if (scheduledDateTime.isAfter(now.plusHours(2))) {
                return "online";
            } else {
                return "offline";
            }
        } catch (DateTimeParseException | IllegalArgumentException e) {
            return "offline";
        }
    }

    private LocalTime parseTimeSlotStart(String timeSlot) {
        String[] parts = timeSlot.split("-");
        if (parts.length > 0) {
            return LocalTime.parse(parts[0].trim());
        }
        return LocalTime.parse(timeSlot.trim());
    }

    /**
     * 带乐观锁保护的保存操作，并发冲突时抛出 BusinessException
     */
    private Registration saveWithOptimisticLock(Registration entity, String operation) {
        try {
            return registrationRepository.save(entity);
        } catch (OptimisticLockException e) {
            throw new BusinessException(GlobalErrorCode.SYSTEM_ERROR,
                    operation + "失败：数据已被其他操作修改，请刷新后重试");
        }
    }
}