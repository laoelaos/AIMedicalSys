package com.aimedical.modules.commonmodule.event;

import java.time.LocalDateTime;

public class RegistrationEvent {

    private Long registrationId;
    private String patientId;
    private String sessionId;
    private String departmentId;
    private String departmentName;
    private Long doctorId;
    private LocalDateTime eventTime;

    public RegistrationEvent() {}

    public RegistrationEvent(Long registrationId, String patientId, String sessionId,
                             String departmentId, String departmentName, Long doctorId,
                             LocalDateTime eventTime) {
        this.registrationId = registrationId;
        this.patientId = patientId;
        this.sessionId = sessionId;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.doctorId = doctorId;
        this.eventTime = eventTime;
    }

    public Long getRegistrationId() { return registrationId; }
    public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }
}
