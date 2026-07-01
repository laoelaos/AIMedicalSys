package com.aimedical.modules.medicalrecord.event;

public class TemplateConfigChangeEvent {

    private final String departmentId;

    public TemplateConfigChangeEvent(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentId() {
        return departmentId;
    }
}
