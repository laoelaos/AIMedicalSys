package com.aimedical.modules.prescription.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.prescription.dto.audit.AuditRequest;
import com.aimedical.modules.prescription.dto.audit.AuditResponse;
import com.aimedical.modules.prescription.dto.audit.BlockResponse;
import com.aimedical.modules.prescription.dto.audit.SubmitRequest;
import com.aimedical.modules.prescription.dto.audit.SubmitResponse;
import com.aimedical.modules.prescription.dto.audit.AuditAlert;
import com.aimedical.modules.prescription.service.audit.AuditRiskLevel;
import com.aimedical.modules.prescription.service.audit.PrescriptionAuditEnforcer;
import com.aimedical.modules.prescription.service.audit.PrescriptionAuditService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/prescription")
public class PrescriptionAuditController {

    private final PrescriptionAuditService prescriptionAuditService;
    private final PrescriptionAuditEnforcer prescriptionAuditEnforcer;

    public PrescriptionAuditController(PrescriptionAuditService prescriptionAuditService,
                                        PrescriptionAuditEnforcer prescriptionAuditEnforcer) {
        this.prescriptionAuditService = prescriptionAuditService;
        this.prescriptionAuditEnforcer = prescriptionAuditEnforcer;
    }

    @PostMapping("/audit")
    public ResponseEntity<Result<AuditResponse>> audit(@Valid @RequestBody AuditRequest request) {
        AuditResponse response = prescriptionAuditService.audit(request);
        if (response.getRiskLevel() == AuditRiskLevel.BLOCK) {
            List<String> reasons = response.getAlerts() != null && !response.getAlerts().isEmpty()
                    ? response.getAlerts().stream().map(AuditAlert::getAlertMessage).collect(Collectors.toList())
                    : List.of("Prescription audit blocked");
            BlockResponse blockInfo = prescriptionAuditEnforcer.enforce(
                    request.getPrescriptionId(),
                    reasons,
                    "RX_BLOCK_AUDIT");
            return ResponseEntity.status(422)
                    .body(Result.fail(blockInfo.getBlockCode(),
                            String.join(", ", blockInfo.getBlockReasons())));
        }
        return ResponseEntity.ok(Result.success(response));
    }

    @PostMapping("/submit")
    public ResponseEntity<Result<SubmitResponse>> submit(@Valid @RequestBody SubmitRequest request) {
        SubmitResponse response = prescriptionAuditService.submit(request);
        if (!response.isSubmitted() && response.getBlockInfo() != null) {
            return ResponseEntity.status(422)
                    .body(Result.fail(response.getBlockInfo().getBlockCode(),
                            "Blocked: " + String.join(", ", response.getBlockInfo().getBlockReasons())));
        }
        if (!response.isSubmitted() && response.getErrorCode() != null) {
            return ResponseEntity.badRequest()
                    .body(Result.fail(response.getErrorCode(), "Submission validation failed"));
        }
        return ResponseEntity.ok(Result.success(response));
    }

    @PostMapping("/audit/{auditId}/revoke")
    public ResponseEntity<Void> revoke(@PathVariable Long auditId) {
        prescriptionAuditService.revoke(auditId);
        return ResponseEntity.ok().build();
    }
}
