package com.aimedical.modules.patient.dto;

public class RegistrationResponse {

    private Long id;
    private String registrationType;
    private String doctorName;
    private String departmentName;
    private String examItemName;
    private String timeSlot;
    private String status;
    private boolean canCancel;
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getRegistrationType() { return registrationType; }
    public void setRegistrationType(String v) { this.registrationType = v; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String v) { this.doctorName = v; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String v) { this.departmentName = v; }
    public String getExamItemName() { return examItemName; }
    public void setExamItemName(String v) { this.examItemName = v; }
    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String v) { this.timeSlot = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public boolean isCanCancel() { return canCancel; }
    public void setCanCancel(boolean v) { this.canCancel = v; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String v) { this.createdAt = v; }
}
