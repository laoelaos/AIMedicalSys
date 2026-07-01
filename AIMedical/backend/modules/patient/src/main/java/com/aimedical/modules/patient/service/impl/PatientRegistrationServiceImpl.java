package com.aimedical.modules.patient.service.impl;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.modules.patient.dto.CancelResponse;
import com.aimedical.modules.patient.dto.RegistrationRequest;
import com.aimedical.modules.patient.dto.RegistrationResponse;
import com.aimedical.modules.patient.entity.RegistrationEntity;
import com.aimedical.modules.patient.entity.RegistrationStatus;
import com.aimedical.modules.patient.repository.PatientRegistrationRepository;
import com.aimedical.modules.patient.service.PatientRegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PatientRegistrationServiceImpl implements PatientRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(PatientRegistrationServiceImpl.class);
    private static final Map<String, Double> REFUND_MAP = Map.of(
            "OUTPATIENT", 15.0, "EXAMINATION", 100.0, "EMERGENCY", 30.0);

    private final PatientRegistrationRepository registrationRepository;

    public PatientRegistrationServiceImpl(PatientRegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public RegistrationResponse create(RegistrationRequest req, Long userId) {
        validateRequest(req);
        if (req.getTimeSlot() != null
                && registrationRepository.existsByUserIdAndTimeSlotAndStatusNotAndDeletedFalse(
                        userId, req.getTimeSlot(), RegistrationStatus.CANCELLED.name())) {
            throw new BusinessException(GlobalErrorCode.DUPLICATE, "该时段已有有效挂号，请勿重复提交");
        }
        RegistrationEntity entity = new RegistrationEntity();
        entity.setUserId(userId);
        entity.setRegistrationType(req.getRegistrationType());
        entity.setDoctorName(req.getDoctorName());
        entity.setDoctorId(req.getDoctorId());
        entity.setDepartmentName(req.getDepartmentName());
        entity.setDepartmentId(req.getDepartmentId());
        entity.setExamItemName(req.getExamItemName());
        entity.setExamItemId(req.getExamItemId());
        entity.setTriageRecordId(req.getTriageRecordId());
        entity.setTimeSlot(req.getTimeSlot());
        entity.setStatus(RegistrationStatus.PENDING.name());
        entity = registrationRepository.save(entity);
        log.info("Registration created: id={}, userId={}, type={}", entity.getId(), userId, req.getRegistrationType());
        return toResponse(entity);
    }

    private void validateRequest(RegistrationRequest req) {
        if (req.getRegistrationType() == null || req.getRegistrationType().isBlank()) {
            throw new BusinessException(GlobalErrorCode.PARAM_INVALID, "挂号类型不能为空");
        }
        if ("OUTPATIENT".equals(req.getRegistrationType())) {
            if (req.getDoctorId() == null && (req.getDoctorName() == null || req.getDoctorName().isBlank())) {
                throw new BusinessException(GlobalErrorCode.PARAM_INVALID, "门诊预约必须指定医生");
            }
        } else if ("EXAMINATION".equals(req.getRegistrationType())) {
            if (req.getExamItemId() == null && (req.getExamItemName() == null || req.getExamItemName().isBlank())) {
                throw new BusinessException(GlobalErrorCode.PARAM_INVALID, "检查预约必须指定检查项目");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistrationResponse> listByUser(Long userId) {
        return registrationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CancelResponse cancel(Long regId, Long userId) {
        RegistrationEntity entity = registrationRepository.findById(regId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "挂号记录不存在"));
        if (!entity.getUserId().equals(userId)) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN, "无权操作此挂号记录");
        }
        if (RegistrationStatus.CANCELLED.name().equals(entity.getStatus()))
            return buildErrorResponse("该挂号已取消");
        if (RegistrationStatus.DISPENSED.name().equals(entity.getStatus()))
            return buildErrorResponse("已发药处方请携带至线下窗口办理退费");
        if (RegistrationStatus.CONFIRMED.name().equals(entity.getStatus()))
            return buildErrorResponse("已确认挂号不支持在线取消，请联系线下窗口");
        if (RegistrationStatus.FINISHED.name().equals(entity.getStatus()))
            return buildErrorResponse("该挂号已完成就诊，不可取消");

        if (entity.getTimeSlot() != null) {
            CancelResponse tc = checkTimeWindow(entity.getTimeSlot());
            if (tc != null) return tc;
        }
        CancelResponse resp = new CancelResponse();
        resp.setSuccess(true);
        resp.setMessage("挂号已成功取消");
        resp.setRefundAmount(REFUND_MAP.getOrDefault(entity.getRegistrationType(), 0.0));
        entity.setStatus(RegistrationStatus.CANCELLED.name());
        try {
            registrationRepository.save(entity);
        } catch (OptimisticLockingFailureException e) {
            throw new BusinessException(GlobalErrorCode.DUPLICATE,
                    "操作冲突，该挂号已被其他操作修改，请刷新后重试");
        }
        log.info("Registration cancelled: id={}, userId={}", regId, userId);
        return resp;
    }

    private CancelResponse buildErrorResponse(String message) {
        CancelResponse resp = new CancelResponse();
        resp.setSuccess(false);
        resp.setMessage(message);
        return resp;
    }

    private CancelResponse checkTimeWindow(String timeSlot) {
        try {
            String[] parts = timeSlot.split(" ");
            if (parts.length < 2) return null;
            String datePart = parts[0], startTime = parts[1].split("-")[0];
            int year = LocalDateTime.now().getYear();
            LocalDateTime slotDateTime = LocalDateTime.parse(
                    year + "-" + datePart + "T" + startTime + ":00",
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            if (slotDateTime.isBefore(LocalDateTime.now())) slotDateTime = slotDateTime.plusYears(1);
            if (ChronoUnit.HOURS.between(LocalDateTime.now(), slotDateTime) < 2) {
                CancelResponse resp = new CancelResponse();
                resp.setSuccess(false);
                resp.setOverWindow(true);
                resp.setMessage("已超过自助取消时间窗，请联系线下窗口");
                return resp;
            }
        } catch (Exception e) { log.warn("Failed to parse time_slot: {}", timeSlot, e); }
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
        r.setCanCancel(RegistrationStatus.PENDING.name().equals(e.getStatus()));
        r.setCreatedAt(e.getCreatedAt() != null
                ? e.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
        return r;
    }
}
