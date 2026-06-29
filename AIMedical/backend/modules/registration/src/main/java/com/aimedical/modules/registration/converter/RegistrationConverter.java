package com.aimedical.modules.registration.converter;

import com.aimedical.modules.registration.dto.RegistrationDTO;
import com.aimedical.modules.registration.dto.TriageRecordDTO;
import com.aimedical.modules.registration.entity.Registration;
import com.aimedical.modules.registration.entity.TriageRecord;

public class RegistrationConverter {

    private RegistrationConverter() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static RegistrationDTO toRegistrationDTO(Registration entity) {
        if (entity == null) {
            return null;
        }
        RegistrationDTO dto = new RegistrationDTO();
        dto.setId(entity.getId());
        dto.setPatientId(entity.getPatientId());
        dto.setDoctorId(entity.getDoctorId());
        dto.setRegistrationType(entity.getRegistrationType());
        dto.setDepartment(entity.getDepartment());
        dto.setScheduledDate(entity.getScheduledDate());
        dto.setScheduledTimeSlot(entity.getScheduledTimeSlot());
        dto.setStatus(entity.getStatus());
        dto.setCancelReason(entity.getCancelReason());
        dto.setCancelTime(entity.getCancelTime());
        dto.setCancelType(entity.getCancelType());
        dto.setTriageLevel(entity.getTriageLevel());
        dto.setChiefComplaint(entity.getChiefComplaint());
        dto.setRegistrationFee(entity.getRegistrationFee());
        dto.setQueueNumber(entity.getQueueNumber());
        dto.setRemark(entity.getRemark());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static Registration toRegistrationEntity(RegistrationDTO dto) {
        if (dto == null) {
            return null;
        }
        Registration entity = new Registration();
        entity.setPatientId(dto.getPatientId());
        entity.setDoctorId(dto.getDoctorId());
        entity.setRegistrationType(dto.getRegistrationType());
        entity.setDepartment(dto.getDepartment());
        entity.setScheduledDate(dto.getScheduledDate());
        entity.setScheduledTimeSlot(dto.getScheduledTimeSlot());
        entity.setStatus(dto.getStatus());
        entity.setCancelReason(dto.getCancelReason());
        entity.setCancelTime(dto.getCancelTime());
        entity.setCancelType(dto.getCancelType());
        entity.setTriageLevel(dto.getTriageLevel());
        entity.setChiefComplaint(dto.getChiefComplaint());
        entity.setRegistrationFee(dto.getRegistrationFee());
        entity.setQueueNumber(dto.getQueueNumber());
        entity.setRemark(dto.getRemark());
        return entity;
    }

    public static TriageRecordDTO toTriageRecordDTO(TriageRecord entity) {
        if (entity == null) {
            return null;
        }
        TriageRecordDTO dto = new TriageRecordDTO();
        dto.setId(entity.getId());
        dto.setRegistrationId(entity.getRegistrationId());
        dto.setPatientId(entity.getPatientId());
        dto.setNurseId(entity.getNurseId());
        dto.setSymptoms(entity.getSymptoms());
        dto.setTemperature(entity.getTemperature());
        dto.setBloodPressure(entity.getBloodPressure());
        dto.setHeartRate(entity.getHeartRate());
        dto.setTriageDepartment(entity.getTriageDepartment());
        dto.setTriageLevel(entity.getTriageLevel());
        dto.setTriageNote(entity.getTriageNote());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static TriageRecord toTriageRecordEntity(TriageRecordDTO dto) {
        if (dto == null) {
            return null;
        }
        TriageRecord entity = new TriageRecord();
        entity.setRegistrationId(dto.getRegistrationId());
        entity.setPatientId(dto.getPatientId());
        entity.setNurseId(dto.getNurseId());
        entity.setSymptoms(dto.getSymptoms());
        entity.setTemperature(dto.getTemperature());
        entity.setBloodPressure(dto.getBloodPressure());
        entity.setHeartRate(dto.getHeartRate());
        entity.setTriageDepartment(dto.getTriageDepartment());
        entity.setTriageLevel(dto.getTriageLevel());
        entity.setTriageNote(dto.getTriageNote());
        return entity;
    }
}