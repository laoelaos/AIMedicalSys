package com.aimedical.modules.prescription.service.audit;

import com.aimedical.modules.prescription.dto.audit.AuditRequest;
import com.aimedical.modules.prescription.dto.audit.AuditResponse;
import com.aimedical.modules.prescription.dto.audit.SubmitRequest;
import com.aimedical.modules.prescription.dto.audit.SubmitResponse;

public interface PrescriptionAuditService {

    AuditResponse audit(AuditRequest request);

    SubmitResponse submit(SubmitRequest request);

    void revoke(Long auditId);
}
