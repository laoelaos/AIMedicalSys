package com.aimedical.modules.registration.service.impl;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.modules.registration.converter.RegistrationConverter;
import com.aimedical.modules.registration.dto.CancelRegistrationRequest;
import com.aimedical.modules.registration.dto.RegistrationDTO;
import com.aimedical.modules.registration.dto.TriageRecordDTO;
import com.aimedical.modules.registration.entity.Registration;
import com.aimedical.modules.registration.entity.RegistrationStatus;
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
            throw new BusinessException(GlobalErrorCode.SYSTEM_ERROR,
                    "当前状态不允许确认操作，仅待确认状态可确认");
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
            throw new BusinessException(GlobalErrorCode.SYSTEM_ERROR,
                    "当前状态不允许完成操作，仅已确认状态可完成");
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
            throw new BusinessException(GlobalErrorCode.SYSTEM_ERROR,
                    "当前状态不允许取消操作，仅待确认或已确认状态可取消");
        }

        // Cancel rule engine: check if scheduled time is at least 2 hours away
        String cancelType = determineCancelType(entity.getScheduledDate(), entity.getScheduledTimeSlot());
        if ("offline".equals(cancelType)) {
            throw new BusinessException(GlobalErrorCode.SYSTEM_ERROR,
                    "预约时间距现在不足2小时，无法在线取消，请到窗口线下办理取消");
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
            throw new BusinessException(GlobalErrorCode.SYSTEM_ERROR,
                    "当前状态不允许标记爽约，仅已确认状态可标记爽约");
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
        List<Registration> todayRegistrations = registrationRepository.findByScheduledDate(scheduledDate);
        long count = todayRegistrations.stream()
                .filter(r -> r.getDoctorId() != null && r.getDoctorId().equals(doctorId))
                .count();
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
        // Expected format: "HH:mm-HH:mm" e.g. "09:00-10:00"
        String[] parts = timeSlot.split("-");
        if (parts.length > 0) {
            return LocalTime.parse(parts[0].trim());
        }
        return LocalTime.parse(timeSlot.trim());
    }
}