package com.aimedical.modules.doctor.service.impl;

import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.common.result.Result;
import com.aimedical.modules.doctor.converter.ConsultationQueueConverter;
import com.aimedical.modules.doctor.dto.response.ConsultationQueueResponse;
import com.aimedical.modules.doctor.entity.ConsultationQueueEntity;
import com.aimedical.modules.doctor.entity.ConsultationStatus;
import com.aimedical.modules.doctor.repository.ConsultationQueueRepository;
import com.aimedical.modules.doctor.service.ConsultationQueueService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 接诊/叫号队列服务实现。
 *
 * <p>状态流转：WAITING -> CALLED -> IN_CONSULTATION -> FINISHED；
 * WAITING/CALLED -> SKIPPED（过号）。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Service
public class ConsultationQueueServiceImpl implements ConsultationQueueService {

    private final ConsultationQueueRepository queueRepository;
    private final ConsultationQueueConverter converter;

    public ConsultationQueueServiceImpl(ConsultationQueueRepository queueRepository,
                                       ConsultationQueueConverter converter) {
        this.queueRepository = queueRepository;
        this.converter = converter;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<List<ConsultationQueueResponse>> listMyQueue(Long doctorUserId) {
        List<String> activeStatuses = List.of(
                ConsultationStatus.WAITING.getCode(),
                ConsultationStatus.CALLED.getCode(),
                ConsultationStatus.IN_CONSULTATION.getCode());
        List<ConsultationQueueResponse> list = queueRepository
                .findByDoctorIdAndStatusInOrderByRegisteredAtAsc(doctorUserId, activeStatuses)
                .stream()
                .map(converter::toResponse)
                .toList();
        return Result.success(list);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<List<ConsultationQueueResponse>> listWaiting(Long doctorUserId) {
        List<ConsultationQueueResponse> list = queueRepository
                .findByDoctorIdAndStatusOrderByRegisteredAtAsc(doctorUserId, ConsultationStatus.WAITING.getCode())
                .stream()
                .map(converter::toResponse)
                .toList();
        return Result.success(list);
    }

    @Override
    @Transactional
    public Result<ConsultationQueueResponse> callNext(Long doctorUserId) {
        List<ConsultationQueueEntity> waiting = queueRepository
                .findByDoctorIdAndStatusOrderByRegisteredAtAsc(doctorUserId, ConsultationStatus.WAITING.getCode());
        if (waiting.isEmpty()) {
            return Result.fail(GlobalErrorCode.NOT_FOUND.getCode(), "暂无候诊患者");
        }
        ConsultationQueueEntity entity = waiting.get(0);
        entity.setStatus(ConsultationStatus.CALLED.getCode());
        entity.setCalledAt(LocalDateTime.now());
        ConsultationQueueEntity saved = queueRepository.save(entity);
        return Result.success(converter.toResponse(saved));
    }

    @Override
    @Transactional
    public Result<ConsultationQueueResponse> startConsultation(Long id, Long doctorUserId) {
        ConsultationQueueEntity entity = loadOrFail(id);
        if (entity == null) {
            return Result.fail(GlobalErrorCode.CONSULTATION_NOT_FOUND);
        }
        // 校验叫号记录归属当前医生，防止越权操作他人叫号记录
        if (!entity.getDoctorId().equals(doctorUserId)) {
            return Result.fail(GlobalErrorCode.FORBIDDEN, "无权操作他人叫号记录");
        }
        if (!ConsultationStatus.CALLED.getCode().equals(entity.getStatus())) {
            return Result.fail(GlobalErrorCode.CONSULTATION_NOT_CALLABLE);
        }
        entity.setStatus(ConsultationStatus.IN_CONSULTATION.getCode());
        ConsultationQueueEntity saved = queueRepository.save(entity);
        return Result.success(converter.toResponse(saved));
    }

    @Override
    @Transactional
    public Result<ConsultationQueueResponse> finishConsultation(Long id, Long doctorUserId) {
        ConsultationQueueEntity entity = loadOrFail(id);
        if (entity == null) {
            return Result.fail(GlobalErrorCode.CONSULTATION_NOT_FOUND);
        }
        // 校验叫号记录归属当前医生，防止越权操作他人叫号记录
        if (!entity.getDoctorId().equals(doctorUserId)) {
            return Result.fail(GlobalErrorCode.FORBIDDEN, "无权操作他人叫号记录");
        }
        if (!ConsultationStatus.IN_CONSULTATION.getCode().equals(entity.getStatus())) {
            return Result.fail(GlobalErrorCode.CONSULTATION_NOT_CALLABLE);
        }
        entity.setStatus(ConsultationStatus.FINISHED.getCode());
        entity.setFinishedAt(LocalDateTime.now());
        ConsultationQueueEntity saved = queueRepository.save(entity);
        return Result.success(converter.toResponse(saved));
    }

    @Override
    @Transactional
    public Result<ConsultationQueueResponse> skip(Long id, Long doctorUserId) {
        ConsultationQueueEntity entity = loadOrFail(id);
        if (entity == null) {
            return Result.fail(GlobalErrorCode.CONSULTATION_NOT_FOUND);
        }
        // 校验叫号记录归属当前医生，防止越权操作他人叫号记录
        if (!entity.getDoctorId().equals(doctorUserId)) {
            return Result.fail(GlobalErrorCode.FORBIDDEN, "无权操作他人叫号记录");
        }
        String status = entity.getStatus();
        if (!ConsultationStatus.WAITING.getCode().equals(status)
                && !ConsultationStatus.CALLED.getCode().equals(status)) {
            return Result.fail(GlobalErrorCode.CONSULTATION_NOT_CALLABLE);
        }
        entity.setStatus(ConsultationStatus.SKIPPED.getCode());
        ConsultationQueueEntity saved = queueRepository.save(entity);
        return Result.success(converter.toResponse(saved));
    }

    private ConsultationQueueEntity loadOrFail(Long id) {
        return queueRepository.findById(id).orElse(null);
    }
}
