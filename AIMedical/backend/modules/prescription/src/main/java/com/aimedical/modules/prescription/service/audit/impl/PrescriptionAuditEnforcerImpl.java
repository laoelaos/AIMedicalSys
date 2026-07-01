package com.aimedical.modules.prescription.service.audit.impl;

import com.aimedical.modules.prescription.dto.audit.BlockResponse;
import com.aimedical.modules.prescription.service.audit.PrescriptionAuditEnforcer;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class PrescriptionAuditEnforcerImpl implements PrescriptionAuditEnforcer {

    @Override
    public BlockResponse enforce(String prescriptionId, List<String> reasons, String blockCode) {
        return new BlockResponse(reasons, blockCode, LocalDateTime.now());
    }
}
