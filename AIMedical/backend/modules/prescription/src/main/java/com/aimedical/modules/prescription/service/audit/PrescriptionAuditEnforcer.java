package com.aimedical.modules.prescription.service.audit;

import com.aimedical.modules.prescription.dto.audit.BlockResponse;
import java.util.List;

public interface PrescriptionAuditEnforcer {

    BlockResponse enforce(String prescriptionId, List<String> reasons, String blockCode);
}
