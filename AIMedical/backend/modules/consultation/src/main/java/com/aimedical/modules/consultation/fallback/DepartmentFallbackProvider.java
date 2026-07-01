package com.aimedical.modules.consultation.fallback;

import com.aimedical.modules.consultation.dto.RecommendedDepartment;
import java.util.List;

public interface DepartmentFallbackProvider {

    List<RecommendedDepartment> getFallbackDepartments();
}
