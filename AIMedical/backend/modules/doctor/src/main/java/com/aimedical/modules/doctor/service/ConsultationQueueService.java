package com.aimedical.modules.doctor.service;

import com.aimedical.common.result.Result;
import com.aimedical.modules.doctor.dto.response.ConsultationQueueResponse;

import java.util.List;

/**
 * 接诊/叫号队列服务。
 *
 * <p>状态流转：WAITING -> CALLED -> IN_CONSULTATION -> FINISHED；
 * WAITING/CALLED -> SKIPPED（过号）。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public interface ConsultationQueueService {

    Result<List<ConsultationQueueResponse>> listMyQueue(Long doctorUserId);

    Result<List<ConsultationQueueResponse>> listWaiting(Long doctorUserId);

    Result<ConsultationQueueResponse> callNext(Long doctorUserId);

    Result<ConsultationQueueResponse> startConsultation(Long id, Long doctorUserId);

    Result<ConsultationQueueResponse> finishConsultation(Long id, Long doctorUserId);

    Result<ConsultationQueueResponse> skip(Long id, Long doctorUserId);
}
