package com.aimedical.modules.registration.service.impl;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.modules.registration.converter.RegistrationConverter;
import com.aimedical.modules.registration.dto.CancelRegistrationRequest;
import com.aimedical.modules.registration.dto.RegistrationDTO;
import com.aimedical.modules.registration.dto.TriageRecordDTO;
import com.aimedical.modules.registration.entity.Registration;
import com.aimedical.modules.registration.entity.RegistrationStatus;
import com.aimedical.modules.registration.entity.RegistrationType;
import com.aimedical.modules.registration.entity.TriageRecord;
import com.aimedical.modules.registration.repository.RegistrationRepository;
import com.aimedical.modules.registration.repository.TriageRecordRepository;
import com.aimedical.modules.registration.service.RegistrationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final TriageRecordRepository triageRecordRepository;

    public RegistrationServiceImpl(RegistrationRepository registrationRepository,
                                   TriageRecordRepository triageRecordRepository) {
        this.registrationRepository = registrationRepository;
        this.triageRecordRepository = triageRecordRepository;
    }

    @Override
    @Transactional
    public RegistrationDTO createRegistration(RegistrationDTO dto) {
        Registration entity = RegistrationConverter.toRegistrationEntity(dto);
        entity.setStatus(RegistrationStatus.PENDING.name());
        entity.setQueueNumber(generateQueueNumber(dto.getDoctorId(), dto.getScheduledDate()));

        // Branch logic based on registration type
        if (dto.getRegistrationType() != null) {
            RegistrationType registrationType = RegistrationType.valueOf(dto.getRegistrationType());
            switch (registrationType) {
                case OUTPATIENT:
                    // 门诊挂号：设置默认科室和分诊级别
                    if (entity.getDepartment() == null && dto.getDepartment() != null) {
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
                    entity.setTriageLevel("LEVEL_1");
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
    public List<RegistrationDTO> getRegistrationsByPatient(Long patientId) {
        List<Registration> entities = registrationRepository.findByPatientId(patientId);
        return entities.stream()
                .map(RegistrationConverter::toRegistrationDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RegistrationDTO> getRegistrationsByDoctor(Long doctorId) {
        List<Registration> entities = registrationRepository.findByDoctorId(doctorId);
        return entities.stream()
                .map(RegistrationConverter::toRegistrationDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RegistrationDTO confirmRegistration(Long id) {
        Registration entity = registrationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND));

        String currentStatus = entity.getStatus();
        if (!RegistrationStatus.PENDING.name().equals(currentStatus)) {
            throw new BusinessException(GlobalErrorCode.REGISTRATION_STATUS_INVALID);
        }

        entity.setStatus(RegistrationStatus.CONFIRMED.name());
        entity = registrationRepository.save(entity);
        return RegistrationConverter.toRegistrationDTO(entity);
    }

    @Override
    @Transactional
    public RegistrationDTO completeRegistration(Long id) {
        Registration entity = registrationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND));

        String currentStatus = entity.getStatus();
        if (!RegistrationStatus.CONFIRMED.name().equals(currentStatus)) {
            throw new BusinessException(GlobalErrorCode.REGISTRATION_STATUS_INVALID);
        }

        entity.setStatus(RegistrationStatus.COMPLETED.name());
        entity = registrationRepository.save(entity);
        return RegistrationConverter.toRegistrationDTO(entity);
    }

    @Override
    @Transactional
    public RegistrationDTO cancelRegistration(Long id, CancelRegistrationRequest request) {
        Registration entity = registrationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND));

        String currentStatus = entity.getStatus();
        if (!RegistrationStatus.PENDING.name().equals(currentStatus)
                && !RegistrationStatus.CONFIRMED.name().equals(currentStatus)) {
            throw new BusinessException(GlobalErrorCode.REGISTRATION_STATUS_INVALID);
        }

        // Cancel rule engine: check if scheduled time is at least 2 hours away
        String cancelType = determineCancelType(entity.getScheduledDate(), entity.getScheduledTimeSlot());
        if ("offline".equals(cancelType)) {
            throw new BusinessException(GlobalErrorCode.REGISTRATION_CANCEL_FORBIDDEN);
        }

        entity.setStatus(RegistrationStatus.CANCELLED.name());
        entity.setCancelReason(request.getCancelReason());
        entity.setCancelTime(LocalDateTime.now());
        entity.setCancelType(cancelType);
        entity = registrationRepository.save(entity);
        return RegistrationConverter.toRegistrationDTO(entity);
    }

    @Override
    @Transactional
    public RegistrationDTO markNoShow(Long id) {
        Registration entity = registrationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND));

        String currentStatus = entity.getStatus();
        if (!RegistrationStatus.CONFIRMED.name().equals(currentStatus)) {
            throw new BusinessException(GlobalErrorCode.REGISTRATION_STATUS_INVALID);
        }

        entity.setStatus(RegistrationStatus.NO_SHOW.name());
        entity = registrationRepository.save(entity);
        return RegistrationConverter.toRegistrationDTO(entity);
    }

    @Override
    @Transactional
    public TriageRecordDTO createTriageRecord(TriageRecordDTO dto) {
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
        long count = registrationRepository.countByScheduledDateAndDoctorId(scheduledDate, doctorId);
        return (int) count + 1;
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
        } catch (Exception e) {
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
}