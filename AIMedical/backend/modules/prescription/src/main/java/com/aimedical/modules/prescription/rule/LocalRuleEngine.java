package com.aimedical.modules.prescription.rule;

import com.aimedical.modules.prescription.dto.audit.AuditRequest;
import java.util.List;

public interface LocalRuleEngine {

    List<LocalRuleResult> check(AuditRequest request);
}
