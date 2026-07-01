package com.aimedical.modules.consultation.fallback;

import com.aimedical.modules.consultation.dto.RecommendedDepartment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StaticDepartmentFallbackProvider implements DepartmentFallbackProvider {

    @Value("${consultation.fallback.departments:}")
    private String fallbackDepartmentsConfig;

    @Override
    public List<RecommendedDepartment> getFallbackDepartments() {
        List<RecommendedDepartment> departments = new ArrayList<>();
        if (fallbackDepartmentsConfig == null || fallbackDepartmentsConfig.isBlank()) {
            return departments;
        }
        String[] entries = fallbackDepartmentsConfig.split(",");
        for (String entry : entries) {
            String[] parts = entry.trim().split(":");
            if (parts.length >= 2) {
                departments.add(new RecommendedDepartment(parts[0].trim(), parts[1].trim(), 0f));
            }
        }
        return departments;
    }
}
