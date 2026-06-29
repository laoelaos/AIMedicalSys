package com.aimedical.modules.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RegistrationRequest {

    @NotBlank
    private String registrationType;

    private Long doctorId;
    private String doctorName;
    private Long departmentId;
    private String departmentName;
    private Long timeSlotId;
    private String timeSlot;
    private Long examItemId;
    private String examItemName;
    private String examCategory;

    public String getRegistrationType() { return registrationType; }
    public void setRegistrationType(String v) { this.registrationType = v; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long v) { this.doctorId = v; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String v) { this.doctorName = v; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long v) { this.departmentId = v; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String v) { this.departmentName = v; }
    public Long getTimeSlotId() { return timeSlotId; }
    public void setTimeSlotId(Long v) { this.timeSlotId = v; }
    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String v) { this.timeSlot = v; }
    public Long getExamItemId() { return examItemId; }
    public void setExamItemId(Long v) { this.examItemId = v; }
    public String getExamItemName() { return examItemName; }
    public void setExamItemName(String v) { this.examItemName = v; }
    public String getExamCategory() { return examCategory; }
    public void setExamCategory(String v) { this.examCategory = v; }
}
