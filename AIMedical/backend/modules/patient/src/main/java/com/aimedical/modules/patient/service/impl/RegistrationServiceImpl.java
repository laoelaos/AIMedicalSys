package com.aimedical.modules.patient.service.impl;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.modules.patient.dto.CancelResponse;
import com.aimedical.modules.patient.dto.RegistrationRequest;
import com.aimedical.modules.patient.dto.RegistrationResponse;
import com.aimedical.modules.patient.entity.RegistrationEntity;
import com.aimedical.modules.patient.repository.RegistrationRepository;
import com.aimedical.modules.patient.service.RegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationServiceImpl.class);

    private final RegistrationRepository registrationRepository;

    public RegistrationServiceImpl(RegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    @Override
    @Transactional
    public RegistrationResponse create(RegistrationRequest req, Long userId) {
        if (req.getTimeSlot() != null
                && registrationRepository.existsByUserIdAndTimeSlotAndStatusNotAndDeletedFalse(
                        userId, req.getTimeSlot(), "CANCELLED")) {
            throw new BusinessException(GlobalErrorCode.DUPLICATE, "该时段已有有效挂号，请勿重复提交");
        }

        RegistrationEntity entity = new RegistrationEntity();
        entity.setUserId(userId);
        entity.setRegistrationType(req.getRegistrationType());
        entity.setDoctorName(req.getDoctorName());
        entity.setDoctorId(req.getDoctorId());
        entity.setDepartmentName(req.getDepartmentName());
        entity.setExamItemName(req.getExamItemName());
        entity.setExamItemId(req.getExamItemId());
        entity.setTimeSlot(req.getTimeSlot());
        entity.setStatus("PENDING");
        entity = registrationRepository.save(entity);
        log.info("Registration created: id={}, userId={}, type={}", entity.getId(), userId, req.getRegistrationType());
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistrationResponse> listByUser(Long userId) {
        return registrationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CancelResponse cancel(Long regId, Long userId) {
        RegistrationEntity entity = registrationRepository.findById(regId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "挂号记录不存在"));

        if (!entity.getUserId().equals(userId)) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN, "无权操作此挂号记录");
        }

        if ("CANCELLED".equals(entity.getStatus())) {
            CancelResponse resp = new CancelResponse();
            resp.setSuccess(false);
            resp.setMessage("该挂号已取消");
            return resp;
        }

        if ("DISPENSED".equals(entity.getStatus())) {
            CancelResponse resp = new CancelResponse();
            resp.setSuccess(false);
            resp.setMessage("已发药处方请携带至线下窗口办理退费");
            return resp;
        }

        if ("CONFIRMED".equals(entity.getStatus())) {
            CancelResponse resp = new CancelResponse();
            resp.setSuccess(false);
            resp.setMessage("已确认挂号不支持在线取消，请联系线下窗口");
            return resp;
        }

        if (entity.getTimeSlot() != null) {
            CancelResponse timeWindowCheck = checkTimeWindow(entity.getTimeSlot());
            if (timeWindowCheck != null) {
                return timeWindowCheck;
            }
        }

        CancelResponse resp = new CancelResponse();
        resp.setSuccess(true);
        resp.setMessage("挂号已成功取消");

        if ("OUTPATIENT".equals(entity.getRegistrationType())) {
            resp.setRefundAmount(15.0);
        } else {
            resp.setRefundAmount(100.0);
        }

        entity.setStatus("CANCELLED");
        registrationRepository.save(entity);
        log.info("Registration cancelled: id={}, userId={}", regId, userId);
        return resp;
    }

    private CancelResponse checkTimeWindow(String timeSlot) {
        try {
            String[] parts = timeSlot.split(" ");
            if (parts.length < 2) return null;
            String datePart = parts[0];
            String startTime = parts[1].split("-")[0];

            int year = LocalDateTime.now().getYear();
            LocalDateTime slotDateTime = LocalDateTime.parse(
                    year + "-" + datePart + "T" + startTime + ":00",
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            );

            long hoursUntil = ChronoUnit.HOURS.between(LocalDateTime.now(), slotDateTime);
            if (hoursUntil < 2) {
                CancelResponse resp = new CancelResponse();
                resp.setSuccess(false);
                resp.setOverWindow(true);
                resp.setMessage("已超过自助取消时间窗，请联系线下窗口");
                return resp;
            }
        } catch (Exception e) {
            log.warn("Failed to parse time_slot for window check: {}", timeSlot, e);
        }
        return null;
    }

    private RegistrationResponse toResponse(RegistrationEntity e) {
        RegistrationResponse r = new RegistrationResponse();
        r.setId(e.getId());
        r.setRegistrationType(e.getRegistrationType());
        r.setDoctorName(e.getDoctorName());
        r.setDepartmentName(e.getDepartmentName());
        r.setExamItemName(e.getExamItemName());
        r.setTimeSlot(e.getTimeSlot());
        r.setStatus(e.getStatus());
        r.setCanCancel("PENDING".equals(e.getStatus()));
        r.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
        return r;
    }
}
