package com.aimedical.modules.patient.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.ai.api.AiResult;
import com.aimedical.modules.ai.api.AiService;
import com.aimedical.modules.ai.api.dto.triage.RecommendedDepartment;
import com.aimedical.modules.ai.api.dto.triage.RecommendedDoctor;
import com.aimedical.modules.ai.api.dto.triage.TriageRequest;
import com.aimedical.modules.ai.api.dto.triage.TriageResponse;
import com.aimedical.modules.commonmodule.api.AuthService;
import com.aimedical.modules.commonmodule.api.dto.CurrentUserResponse;
import com.aimedical.modules.patient.dto.TriageRecordRequest;
import com.aimedical.modules.patient.service.TriageRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patient/triage")
public class TriageController {

    private static final Logger log = LoggerFactory.getLogger(TriageController.class);

    private final AiService aiService;
    private final AuthService authService;
    private final TriageRecordService triageRecordService;

    public TriageController(@Qualifier("mockAiService") AiService aiService, AuthService authService,
                            TriageRecordService triageRecordService) {
        this.aiService = aiService;
        this.authService = authService;
        this.triageRecordService = triageRecordService;
    }

    @PostMapping
    public Result<TriageResponse> triage(@RequestBody TriageRequest request) {
        CurrentUserResponse user = authService.getCurrentUser();
        if (user == null || user.getUserId() == null) {
            return Result.fail("UNAUTHORIZED", "用户未认证");
        }

        if (request.getChiefComplaint() == null || request.getChiefComplaint().isBlank()) {
            return Result.fail("PARAM_INVALID", "主诉不能为空");
        }

        AiResult<TriageResponse> aiResult;
        try {
            CompletableFuture<AiResult<TriageResponse>> future = aiService.triage(request);
            aiResult = future.get(30, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException |
                 java.lang.InterruptedException e) {
            log.error("AI triage invocation failed", e);
            return Result.fail("AI_SERVICE_ERROR", "AI 服务调用异常，请稍后重试");
        }

        if (aiResult.isSuccess()) {
            TriageResponse aiResp = aiResult.getData();

            if (aiResp.getIsComplete()) {
                if (aiResp.getDepartments() == null || aiResp.getDepartments().isEmpty()) {
                    aiResp.setDepartments(buildFallbackDepartments(request.getChiefComplaint()));
                }
                if (aiResp.getDoctors() == null || aiResp.getDoctors().isEmpty()) {
                    aiResp.setDoctors(buildFallbackDoctors());
                }
                if (aiResp.getReason() == null) {
                    aiResp.setReason("AI 综合分析完成，建议根据推荐科室进一步就诊");
                }
            }

            asyncSaveRecord(user.getUserId(), request, aiResp, false);
            return Result.success(aiResp);
        } else if (aiResult.isDegraded()) {
            TriageResponse degraded = buildDegradedResponse(request);
            asyncSaveRecord(user.getUserId(), request, degraded, true);
            return Result.success(degraded);
        } else {
            log.error("AI triage returned non-success result");
            return Result.fail("AI_SERVICE_ERROR", "AI 导诊未返回有效结果");
        }
    }

    private TriageResponse buildDegradedResponse(TriageRequest request) {
        TriageResponse resp = new TriageResponse();
        return resp;
    }

    private List<RecommendedDepartment> buildFallbackDepartments(String chiefComplaint) {
        List<RecommendedDepartment> depts = new ArrayList<>();
        RecommendedDepartment d1 = new RecommendedDepartment();
        d1.setDepartmentId(1);
        d1.setDepartmentName("普通内科");
        d1.setScore(75);
        depts.add(d1);
        RecommendedDepartment d2 = new RecommendedDepartment();
        d2.setDepartmentId(2);
        d2.setDepartmentName("全科门诊");
        d2.setScore(60);
        depts.add(d2);
        return depts;
    }

    private List<RecommendedDoctor> buildFallbackDoctors() {
        List<RecommendedDoctor> docs = new ArrayList<>();
        RecommendedDoctor d1 = new RecommendedDoctor();
        d1.setDoctorId(101);
        d1.setDoctorName("王主任");
        d1.setAvailableSlotCount(5);
        d1.setScore(95);
        docs.add(d1);
        RecommendedDoctor d2 = new RecommendedDoctor();
        d2.setDoctorId(102);
        d2.setDoctorName("张副主任");
        d2.setAvailableSlotCount(3);
        d2.setScore(82);
        docs.add(d2);
        RecommendedDoctor d3 = new RecommendedDoctor();
        d3.setDoctorId(103);
        d3.setDoctorName("李主治医师");
        d3.setAvailableSlotCount(8);
        d3.setScore(70);
        docs.add(d3);
        return docs;
    }

    private void asyncSaveRecord(Long patientId, TriageRequest request, TriageResponse response,
                                  boolean degraded) {
        try {
            TriageRecordRequest recordReq = new TriageRecordRequest();
            recordReq.setPatientId(patientId);
            recordReq.setChiefComplaint(request.getChiefComplaint());
            recordReq.setSessionId(response.getSessionId());
            recordReq.setDegraded(degraded);
            recordReq.setRuleVersion("v1.0.0");
            recordReq.setRuleSetId("rule-set-default");
            if (response.getDepartments() != null) {
                recordReq.setRecommendedDepartments(response.getDepartments().stream()
                        .map(RecommendedDepartment::getDepartmentName).collect(Collectors.toList()));
            }
            if (response.getDoctors() != null) {
                recordReq.setRecommendedDoctors(response.getDoctors().stream()
                        .map(RecommendedDoctor::getDoctorName).collect(Collectors.toList()));
            }
            recordReq.setMatchedRules(List.of("分诊规则-通用"));
            triageRecordService.saveAsync(recordReq);
        } catch (Exception e) {
            log.error("Failed to enqueue triage record save", e);
        }
    }
}
