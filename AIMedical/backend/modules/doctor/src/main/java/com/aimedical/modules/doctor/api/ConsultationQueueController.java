package com.aimedical.modules.doctor.api;

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
        return queueService.listMyQueue(currentDoctorId());
    }

    /**
     * 查询当前医生的候诊队列（仅 WAITING）。
     */
    @GetMapping("/waiting")
    public Result<List<ConsultationQueueResponse>> listWaiting() {
        return queueService.listWaiting(currentDoctorId());
    }

    /**
     * 叫下一位患者（WAITING -> CALLED）。
     */
    @PostMapping("/call-next")
    public Result<ConsultationQueueResponse> callNext() {
        return queueService.callNext(currentDoctorId());
    }

    /**
     * 开始接诊（CALLED -> IN_CONSULTATION）。
     */
    @PostMapping("/{id}/start")
    public Result<ConsultationQueueResponse> startConsultation(@PathVariable Long id) {
        return queueService.startConsultation(id, currentDoctorId());
    }

    /**
     * 完成接诊（IN_CONSULTATION -> FINISHED）。
     */
    @PostMapping("/{id}/finish")
    public Result<ConsultationQueueResponse> finishConsultation(@PathVariable Long id) {
        return queueService.finishConsultation(id, currentDoctorId());
    }

    /**
     * 过号（WAITING/CALLED -> SKIPPED）。
     */
    @PostMapping("/{id}/skip")
    public Result<ConsultationQueueResponse> skip(@PathVariable Long id) {
        return queueService.skip(id, currentDoctorId());
    }

    private Long currentDoctorId() {
        Long userId = currentUser.getUserId();
        if (userId == null) {
            throw new IllegalStateException("无法获取当前登录医生ID");
        }
        return userId;
    }
}
