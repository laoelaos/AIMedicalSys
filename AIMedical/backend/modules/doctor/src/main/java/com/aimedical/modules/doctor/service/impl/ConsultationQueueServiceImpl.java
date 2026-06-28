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
import java.util.Optional;

/**
 * 接诊/叫号队列服务实现。
 *
 * <p>状态流转：WAITING -> CALLED -> IN_CONSULTATION -> FINISHED；
 * WAITING/CALLED -> SKIPPED（过号）。
 *
 * <p>并发约束：同一医生同一时间仅允许一名患者处于 CALLED/IN_CONSULTATION，
 * 叫号前校验，避免并发接诊冲突。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Service
public class ConsultationQueueServiceImpl implements ConsultationQueueService {

    /** 同医生活跃状态（已叫号/接诊中），用于并发接诊约束校验 */
    private static final List<String> ACTIVE_STATUSES = List.of(
            ConsultationStatus.CALLED.getCode(),
            ConsultationStatus.IN_CONSULTATION.getCode());

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
        // 并发约束：同一医生已有 CALLED/IN_CONSULTATION 患者时禁止再次叫号，
        // 避免同一医生并发接诊多名患者
        if (!queueRepository
                .findByDoctorIdAndStatusInOrderByRegisteredAtAsc(doctorUserId, ACTIVE_STATUSES)
                .isEmpty()) {
            return Result.fail(GlobalErrorCode.CONSULTATION_NOT_CALLABLE.getCode(),
                    "当前已有叫号/接诊中患者，请先完成该接诊后再叫下一位");
        }
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
        Optional<ConsultationQueueEntity> opt = loadOrFail(id);
        if (opt.isEmpty()) {
            return Result.fail(GlobalErrorCode.CONSULTATION_NOT_FOUND);
        }
        ConsultationQueueEntity entity = opt.get();
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
        Optional<ConsultationQueueEntity> opt = loadOrFail(id);
        if (opt.isEmpty()) {
            return Result.fail(GlobalErrorCode.CONSULTATION_NOT_FOUND);
        }
        ConsultationQueueEntity entity = opt.get();
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
        Optional<ConsultationQueueEntity> opt = loadOrFail(id);
        if (opt.isEmpty()) {
            return Result.fail(GlobalErrorCode.CONSULTATION_NOT_FOUND);
        }
        ConsultationQueueEntity entity = opt.get();
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

    /**
     * 加载叫号记录，找不到返回 Optional.empty() 而非 null，
     * 调用方须显式处理缺失场景，避免 NPE。
     */
    private Optional<ConsultationQueueEntity> loadOrFail(Long id) {
        return queueRepository.findById(id);
    }
}
