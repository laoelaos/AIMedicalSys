package com.aimedical.modules.medicalorder.dto;

import com.aimedical.modules.medicalorder.entity.OrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class MedicalOrderCreateRequest {

    @NotNull(message = "患者ID不能为空")
    private Long patientId;

    @NotNull(message = "医生ID不能为空")
    private Long doctorId;

    @NotNull(message = "挂号记录ID不能为空")
    private Long registrationId;

    @NotNull(message = "医嘱类型不能为空")
    private OrderType orderType;

    private String diagnosis;

    private Boolean isUrgent;

    private String remark;

    @Valid
    private List<MedicalOrderItemDTO> items;

}
