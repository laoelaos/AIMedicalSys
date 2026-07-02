package com.aimedical.modules.doctor.api;

import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.auth.CurrentUser;
import com.aimedical.modules.doctor.dto.response.ConsultationQueueResponse;
import com.aimedical.modules.doctor.service.ConsultationQueueService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 接诊/叫号队列控制器（挂号管理）。
 *
 * <p>提供医生查看自己队列、叫号、开始接诊、完成接诊、过号等接口。
 * 全部需要 DOCTOR 角色。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/doctor/queue")
@PreAuthorize("hasRole('DOCTOR')")
public class ConsultationQueueController {

    private final ConsultationQueueService queueService;
    private final CurrentUser currentUser;

    public ConsultationQueueController(ConsultationQueueService queueService, CurrentUser currentUser) {
        this.queueService = queueService;
        this.currentUser = currentUser;
    }

    /**
     * 查询当前医生的活跃队列（候诊+已叫号+接诊中）。
     */
    @GetMapping
    public Result<List<ConsultationQueueResponse>> listMyQueue() {
        Long doctorId = currentDoctorId();
        if (doctorId == null) {
            return Result.fail(GlobalErrorCode.UNAUTHORIZED.getCode(), "无法获取当前登录医生ID");
        }
        return queueService.listMyQueue(doctorId);
    }

    /**
     * 查询当前医生的候诊队列（仅 WAITING）。
     */
    @GetMapping("/waiting")
    public Result<List<ConsultationQueueResponse>> listWaiting() {
        Long doctorId = currentDoctorId();
        if (doctorId == null) {
            return Result.fail(GlobalErrorCode.UNAUTHORIZED.getCode(), "无法获取当前登录医生ID");
        }
        return queueService.listWaiting(doctorId);
    }

    /**
     * 叫下一位患者（WAITING -> CALLED）。
     */
    @PostMapping("/call-next")
    public Result<ConsultationQueueResponse> callNext() {
        Long doctorId = currentDoctorId();
        if (doctorId == null) {
            return Result.fail(GlobalErrorCode.UNAUTHORIZED.getCode(), "无法获取当前登录医生ID");
        }
        return queueService.callNext(doctorId);
    }

    /**
     * 开始接诊（CALLED -> IN_CONSULTATION）。
     */
    @PostMapping("/{id}/start")
    public Result<ConsultationQueueResponse> startConsultation(@PathVariable Long id) {
        Long doctorId = currentDoctorId();
        if (doctorId == null) {
            return Result.fail(GlobalErrorCode.UNAUTHORIZED.getCode(), "无法获取当前登录医生ID");
        }
        return queueService.startConsultation(id, doctorId);
    }

    /**
     * 完成接诊（IN_CONSULTATION -> FINISHED）。
     */
    @PostMapping("/{id}/finish")
    public Result<ConsultationQueueResponse> finishConsultation(@PathVariable Long id) {
        Long doctorId = currentDoctorId();
        if (doctorId == null) {
            return Result.fail(GlobalErrorCode.UNAUTHORIZED.getCode(), "无法获取当前登录医生ID");
        }
        return queueService.finishConsultation(id, doctorId);
    }

    /**
     * 过号（WAITING/CALLED -> SKIPPED）。
     */
    @PostMapping("/{id}/skip")
    public Result<ConsultationQueueResponse> skip(@PathVariable Long id) {
        Long doctorId = currentDoctorId();
        if (doctorId == null) {
            return Result.fail(GlobalErrorCode.UNAUTHORIZED.getCode(), "无法获取当前登录医生ID");
        }
        return queueService.skip(id, doctorId);
    }

    /**
     * 获取当前登录医生 ID，未登录返回 null（由调用方判断并返回 UNAUTHORIZED 业务错误）。
     */
    private Long currentDoctorId() {
        return currentUser.getUserId();
    }
}
