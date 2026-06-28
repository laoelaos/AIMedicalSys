package com.aimedical.modules.medicalorder.dto;

import com.aimedical.modules.medicalorder.entity.ItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MedicalOrderItemDTO {

    private Long id;
    private Long orderId;

    @NotNull(message = "项目类型不能为空")
    private ItemType itemType;

    @NotBlank(message = "项目编码不能为空")
    private String itemCode;

    @NotBlank(message = "项目名称不能为空")
    private String itemName;

    private String specification;

    @NotNull(message = "数量不能为空")
    private BigDecimal quantity;

    private String unit;

    @NotNull(message = "单价不能为空")
    private BigDecimal unitPrice;

    private BigDecimal amount;
    private String dosage;
    private String usageMethod;
    private String frequency;
    private Integer days;
    private String remark;

}
