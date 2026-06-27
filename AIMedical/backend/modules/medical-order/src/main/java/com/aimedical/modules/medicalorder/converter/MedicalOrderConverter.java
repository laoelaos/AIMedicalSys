package com.aimedical.modules.medicalorder.converter;

import com.aimedical.modules.medicalorder.dto.ChargePreOrderDTO;
import com.aimedical.modules.medicalorder.dto.ChargePreOrderItemDTO;
import com.aimedical.modules.medicalorder.dto.MedicalOrderDTO;
import com.aimedical.modules.medicalorder.dto.MedicalOrderItemDTO;
import com.aimedical.modules.medicalorder.entity.ChargePreOrder;
import com.aimedical.modules.medicalorder.entity.ChargePreOrderItem;
import com.aimedical.modules.medicalorder.entity.MedicalOrder;
import com.aimedical.modules.medicalorder.entity.MedicalOrderItem;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MedicalOrderConverter {

    public static MedicalOrderDTO toDto(MedicalOrder entity) {
        MedicalOrderDTO dto = new MedicalOrderDTO();
        dto.setId(entity.getId());
        dto.setPatientId(entity.getPatientId());
        dto.setDoctorId(entity.getDoctorId());
        dto.setRegistrationId(entity.getRegistrationId());
        dto.setOrderNo(entity.getOrderNo());
        dto.setOrderType(entity.getOrderType());
        dto.setOrderStatus(entity.getOrderStatus());
        dto.setDiagnosis(entity.getDiagnosis());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setIsUrgent(entity.getIsUrgent());
        dto.setRemark(entity.getRemark());
        return dto;
    }

    public static MedicalOrder toEntity(MedicalOrderDTO dto) {
        MedicalOrder entity = new MedicalOrder();
        entity.setId(dto.getId());
        entity.setPatientId(dto.getPatientId());
        entity.setDoctorId(dto.getDoctorId());
        entity.setRegistrationId(dto.getRegistrationId());
        entity.setOrderNo(dto.getOrderNo());
        entity.setOrderType(dto.getOrderType());
        entity.setOrderStatus(dto.getOrderStatus());
        entity.setDiagnosis(dto.getDiagnosis());
        entity.setTotalAmount(dto.getTotalAmount());
        entity.setIsUrgent(dto.getIsUrgent());
        entity.setRemark(dto.getRemark());
        return entity;
    }

    public static MedicalOrderItemDTO toDto(MedicalOrderItem entity) {
        MedicalOrderItemDTO dto = new MedicalOrderItemDTO();
        dto.setId(entity.getId());
        dto.setOrderId(entity.getOrderId());
        dto.setItemType(entity.getItemType());
        dto.setItemCode(entity.getItemCode());
        dto.setItemName(entity.getItemName());
        dto.setSpecification(entity.getSpecification());
        dto.setQuantity(entity.getQuantity());
        dto.setUnit(entity.getUnit());
        dto.setUnitPrice(entity.getUnitPrice());
        dto.setAmount(entity.getAmount());
        dto.setDosage(entity.getDosage());
        dto.setUsageMethod(entity.getUsageMethod());
        dto.setFrequency(entity.getFrequency());
        dto.setDays(entity.getDays());
        dto.setRemark(entity.getRemark());
        return dto;
    }

    public static MedicalOrderItem toEntity(MedicalOrderItemDTO dto) {
        MedicalOrderItem entity = new MedicalOrderItem();
        entity.setId(dto.getId());
        entity.setOrderId(dto.getOrderId());
        entity.setItemType(dto.getItemType());
        entity.setItemCode(dto.getItemCode());
        entity.setItemName(dto.getItemName());
        entity.setSpecification(dto.getSpecification());
        entity.setQuantity(dto.getQuantity());
        entity.setUnit(dto.getUnit());
        entity.setUnitPrice(dto.getUnitPrice());
        entity.setAmount(dto.getAmount());
        entity.setDosage(dto.getDosage());
        entity.setUsageMethod(dto.getUsageMethod());
        entity.setFrequency(dto.getFrequency());
        entity.setDays(dto.getDays());
        entity.setRemark(dto.getRemark());
        return entity;
    }

    public static ChargePreOrderDTO toDto(ChargePreOrder entity) {
        ChargePreOrderDTO dto = new ChargePreOrderDTO();
        dto.setId(entity.getId());
        dto.setOrderId(entity.getOrderId());
        dto.setPatientId(entity.getPatientId());
        dto.setChargeNo(entity.getChargeNo());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setChargeStatus(entity.getChargeStatus());
        dto.setRemark(entity.getRemark());
        return dto;
    }

    public static ChargePreOrderItemDTO toDto(ChargePreOrderItem entity) {
        ChargePreOrderItemDTO dto = new ChargePreOrderItemDTO();
        dto.setId(entity.getId());
        dto.setChargePreOrderId(entity.getChargePreOrderId());
        dto.setOrderItemId(entity.getOrderItemId());
        dto.setItemName(entity.getItemName());
        dto.setQuantity(entity.getQuantity());
        dto.setUnitPrice(entity.getUnitPrice());
        dto.setAmount(entity.getAmount());
        dto.setChargeItemType(entity.getChargeItemType());
        return dto;
    }

    public static List<MedicalOrderItemDTO> toItemDtoList(List<MedicalOrderItem> items) {
        if (items == null) {
            return Collections.emptyList();
        }
        return items.stream().map(MedicalOrderConverter::toDto).collect(Collectors.toList());
    }

    public static List<ChargePreOrderItemDTO> toChargeItemDtoList(List<ChargePreOrderItem> items) {
        if (items == null) {
            return Collections.emptyList();
        }
        return items.stream().map(MedicalOrderConverter::toDto).collect(Collectors.toList());
    }

}