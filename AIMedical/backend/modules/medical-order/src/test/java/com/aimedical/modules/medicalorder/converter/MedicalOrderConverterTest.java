package com.aimedical.modules.medicalorder.converter;

import com.aimedical.modules.medicalorder.dto.ChargePreOrderDTO;
import com.aimedical.modules.medicalorder.dto.ChargePreOrderItemDTO;
import com.aimedical.modules.medicalorder.dto.MedicalOrderCreateRequest;
import com.aimedical.modules.medicalorder.dto.MedicalOrderDTO;
import com.aimedical.modules.medicalorder.dto.MedicalOrderItemDTO;
import com.aimedical.modules.medicalorder.dto.MedicationOrderDTO;
import com.aimedical.modules.medicalorder.entity.ChargeItemType;
import com.aimedical.modules.medicalorder.entity.ChargePreOrder;
import com.aimedical.modules.medicalorder.entity.ChargePreOrderItem;
import com.aimedical.modules.medicalorder.entity.ChargeStatus;
import com.aimedical.modules.medicalorder.entity.ItemType;
import com.aimedical.modules.medicalorder.entity.MedicalOrder;
import com.aimedical.modules.medicalorder.entity.MedicalOrderItem;
import com.aimedical.modules.medicalorder.entity.OrderStatus;
import com.aimedical.modules.medicalorder.entity.OrderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MedicalOrderConverter 转换器")
class MedicalOrderConverterTest {

    @Test
    @DisplayName("MedicalOrder 实体转 DTO 应完整映射所有字段")
    void shouldConvertOrderEntityToDto() {
        MedicalOrder entity = new MedicalOrder();
        entity.setId(1L);
        entity.setPatientId(10L);
        entity.setDoctorId(20L);
        entity.setRegistrationId(30L);
        entity.setOrderNo("MO-001");
        entity.setOrderType(OrderType.DRUG);
        entity.setOrderStatus(OrderStatus.SUBMITTED);
        entity.setDiagnosis("感冒");
        entity.setTotalAmount(new BigDecimal("100.00"));
        entity.setIsUrgent(true);
        entity.setRemark("紧急");

        MedicalOrderDTO dto = MedicalOrderConverter.toDto(entity);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getPatientId()).isEqualTo(10L);
        assertThat(dto.getDoctorId()).isEqualTo(20L);
        assertThat(dto.getRegistrationId()).isEqualTo(30L);
        assertThat(dto.getOrderNo()).isEqualTo("MO-001");
        assertThat(dto.getOrderType()).isEqualTo(OrderType.DRUG);
        assertThat(dto.getOrderStatus()).isEqualTo(OrderStatus.SUBMITTED);
        assertThat(dto.getDiagnosis()).isEqualTo("感冒");
        assertThat(dto.getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(dto.getIsUrgent()).isTrue();
        assertThat(dto.getRemark()).isEqualTo("紧急");
    }

    @Test
    @DisplayName("MedicalOrderDTO 转 实体应完整映射所有字段")
    void shouldConvertOrderDtoToEntity() {
        MedicalOrderDTO dto = new MedicalOrderDTO();
        dto.setId(1L);
        dto.setPatientId(10L);
        dto.setDoctorId(20L);
        dto.setRegistrationId(30L);
        dto.setOrderNo("MO-002");
        dto.setOrderType(OrderType.EXAMINATION);
        dto.setOrderStatus(OrderStatus.CHARGED);
        dto.setDiagnosis("体检");
        dto.setTotalAmount(new BigDecimal("200.00"));
        dto.setIsUrgent(false);
        dto.setRemark("常规");

        MedicalOrder entity = MedicalOrderConverter.toEntity(dto);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getPatientId()).isEqualTo(10L);
        assertThat(entity.getDoctorId()).isEqualTo(20L);
        assertThat(entity.getRegistrationId()).isEqualTo(30L);
        assertThat(entity.getOrderNo()).isEqualTo("MO-002");
        assertThat(entity.getOrderType()).isEqualTo(OrderType.EXAMINATION);
        assertThat(entity.getOrderStatus()).isEqualTo(OrderStatus.CHARGED);
        assertThat(entity.getDiagnosis()).isEqualTo("体检");
        assertThat(entity.getTotalAmount()).isEqualByComparingTo("200.00");
        assertThat(entity.getIsUrgent()).isFalse();
        assertThat(entity.getRemark()).isEqualTo("常规");
    }

    @Test
    @DisplayName("MedicalOrderCreateRequest 转实体应映射创建请求字段")
    void shouldConvertCreateRequestToEntity() {
        MedicalOrderCreateRequest request = new MedicalOrderCreateRequest();
        request.setPatientId(10L);
        request.setDoctorId(20L);
        request.setRegistrationId(30L);
        request.setOrderType(OrderType.LAB_TEST);
        request.setDiagnosis("化验");
        request.setIsUrgent(true);
        request.setRemark("加急");

        MedicalOrder entity = MedicalOrderConverter.toEntity(request);

        assertThat(entity.getPatientId()).isEqualTo(10L);
        assertThat(entity.getDoctorId()).isEqualTo(20L);
        assertThat(entity.getRegistrationId()).isEqualTo(30L);
        assertThat(entity.getOrderType()).isEqualTo(OrderType.LAB_TEST);
        assertThat(entity.getDiagnosis()).isEqualTo("化验");
        assertThat(entity.getIsUrgent()).isTrue();
        assertThat(entity.getRemark()).isEqualTo("加急");
        // id/orderNo/orderStatus/totalAmount 不在 CreateRequest 中映射
        assertThat(entity.getId()).isNull();
        assertThat(entity.getOrderNo()).isNull();
    }

    @Test
    @DisplayName("MedicalOrderItem 实体转 DTO 应完整映射所有字段")
    void shouldConvertItemEntityToDto() {
        MedicalOrderItem entity = new MedicalOrderItem();
        entity.setId(1L);
        entity.setOrderId(100L);
        entity.setItemType(ItemType.DRUG);
        entity.setItemCode("D001");
        entity.setItemName("阿莫西林");
        entity.setSpecification("0.25g");
        entity.setQuantity(new BigDecimal("2"));
        entity.setUnit("盒");
        entity.setUnitPrice(new BigDecimal("10"));
        entity.setAmount(new BigDecimal("20"));
        entity.setDosage("1粒");
        entity.setUsageMethod("口服");
        entity.setFrequency("每日3次");
        entity.setDays(3);
        entity.setRemark("饭后");

        MedicalOrderItemDTO dto = MedicalOrderConverter.toDto(entity);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getOrderId()).isEqualTo(100L);
        assertThat(dto.getItemType()).isEqualTo(ItemType.DRUG);
        assertThat(dto.getItemCode()).isEqualTo("D001");
        assertThat(dto.getItemName()).isEqualTo("阿莫西林");
        assertThat(dto.getSpecification()).isEqualTo("0.25g");
        assertThat(dto.getQuantity()).isEqualByComparingTo("2");
        assertThat(dto.getUnit()).isEqualTo("盒");
        assertThat(dto.getUnitPrice()).isEqualByComparingTo("10");
        assertThat(dto.getAmount()).isEqualByComparingTo("20");
        assertThat(dto.getDosage()).isEqualTo("1粒");
        assertThat(dto.getUsageMethod()).isEqualTo("口服");
        assertThat(dto.getFrequency()).isEqualTo("每日3次");
        assertThat(dto.getDays()).isEqualTo(3);
        assertThat(dto.getRemark()).isEqualTo("饭后");
    }

    @Test
    @DisplayName("MedicalOrderItemDTO 转实体应完整映射所有字段")
    void shouldConvertItemDtoToEntity() {
        MedicalOrderItemDTO dto = new MedicalOrderItemDTO();
        dto.setId(2L);
        dto.setOrderId(101L);
        dto.setItemType(ItemType.EXAMINATION);
        dto.setItemCode("E001");
        dto.setItemName("血常规");
        dto.setSpecification("常规");
        dto.setQuantity(new BigDecimal("1"));
        dto.setUnit("次");
        dto.setUnitPrice(new BigDecimal("50"));
        dto.setAmount(new BigDecimal("50"));
        dto.setDosage(null);
        dto.setUsageMethod(null);
        dto.setFrequency(null);
        dto.setDays(null);
        dto.setRemark("无");

        MedicalOrderItem entity = MedicalOrderConverter.toEntity(dto);

        assertThat(entity.getId()).isEqualTo(2L);
        assertThat(entity.getOrderId()).isEqualTo(101L);
        assertThat(entity.getItemType()).isEqualTo(ItemType.EXAMINATION);
        assertThat(entity.getItemCode()).isEqualTo("E001");
        assertThat(entity.getItemName()).isEqualTo("血常规");
        assertThat(entity.getSpecification()).isEqualTo("常规");
        assertThat(entity.getQuantity()).isEqualByComparingTo("1");
        assertThat(entity.getUnit()).isEqualTo("次");
        assertThat(entity.getUnitPrice()).isEqualByComparingTo("50");
        assertThat(entity.getAmount()).isEqualByComparingTo("50");
        assertThat(entity.getRemark()).isEqualTo("无");
    }

    @Test
    @DisplayName("ChargePreOrder 实体转 DTO 应完整映射所有字段")
    void shouldConvertChargePreOrderToDto() {
        ChargePreOrder entity = new ChargePreOrder();
        entity.setId(1L);
        entity.setOrderId(100L);
        entity.setPatientId(10L);
        entity.setChargeNo("CP-001");
        entity.setTotalAmount(new BigDecimal("80"));
        entity.setChargeStatus(ChargeStatus.CHARGED);
        entity.setRemark("已收费");

        ChargePreOrderDTO dto = MedicalOrderConverter.toDto(entity);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getOrderId()).isEqualTo(100L);
        assertThat(dto.getPatientId()).isEqualTo(10L);
        assertThat(dto.getChargeNo()).isEqualTo("CP-001");
        assertThat(dto.getTotalAmount()).isEqualByComparingTo("80");
        assertThat(dto.getChargeStatus()).isEqualTo(ChargeStatus.CHARGED);
        assertThat(dto.getRemark()).isEqualTo("已收费");
    }

    @Test
    @DisplayName("ChargePreOrderItem 实体转 DTO 应完整映射所有字段")
    void shouldConvertChargePreOrderItemToDto() {
        ChargePreOrderItem entity = new ChargePreOrderItem();
        entity.setId(1L);
        entity.setChargePreOrderId(500L);
        entity.setOrderItemId(1L);
        entity.setItemName("阿莫西林");
        entity.setQuantity(new BigDecimal("2"));
        entity.setUnitPrice(new BigDecimal("10"));
        entity.setAmount(new BigDecimal("20"));
        entity.setChargeItemType(ChargeItemType.DRUG);

        ChargePreOrderItemDTO dto = MedicalOrderConverter.toDto(entity);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getChargePreOrderId()).isEqualTo(500L);
        assertThat(dto.getOrderItemId()).isEqualTo(1L);
        assertThat(dto.getItemName()).isEqualTo("阿莫西林");
        assertThat(dto.getQuantity()).isEqualByComparingTo("2");
        assertThat(dto.getUnitPrice()).isEqualByComparingTo("10");
        assertThat(dto.getAmount()).isEqualByComparingTo("20");
        assertThat(dto.getChargeItemType()).isEqualTo(ChargeItemType.DRUG);
    }

    @Test
    @DisplayName("toItemDtoList 当入参为 null 时返回空列表")
    void shouldReturnEmptyListWhenItemDtoListNull() {
        assertThat(MedicalOrderConverter.toItemDtoList(null)).isEmpty();
    }

    @Test
    @DisplayName("toItemDtoList 应批量转换明细")
    void shouldConvertItemList() {
        MedicalOrderItem item1 = new MedicalOrderItem();
        item1.setId(1L);
        item1.setItemName("药品1");
        MedicalOrderItem item2 = new MedicalOrderItem();
        item2.setId(2L);
        item2.setItemName("药品2");

        List<MedicalOrderItemDTO> result = MedicalOrderConverter.toItemDtoList(List.of(item1, item2));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(MedicalOrderItemDTO::getItemName)
                .containsExactly("药品1", "药品2");
    }

    @Test
    @DisplayName("toChargeItemDtoList 当入参为 null 时返回空列表")
    void shouldReturnEmptyListWhenChargeItemDtoListNull() {
        assertThat(MedicalOrderConverter.toChargeItemDtoList(null)).isEmpty();
    }

    @Test
    @DisplayName("toChargeItemDtoList 应批量转换收费明细")
    void shouldConvertChargeItemList() {
        ChargePreOrderItem item1 = new ChargePreOrderItem();
        item1.setId(1L);
        item1.setItemName("药品");
        ChargePreOrderItem item2 = new ChargePreOrderItem();
        item2.setId(2L);
        item2.setItemName("检查");

        List<ChargePreOrderItemDTO> result =
                MedicalOrderConverter.toChargeItemDtoList(List.of(item1, item2));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ChargePreOrderItemDTO::getItemName)
                .containsExactly("药品", "检查");
    }

    @Test
    @DisplayName("toMedicationOrderDTO 应构建处方契约（含明细）")
    void shouldBuildMedicationOrderDtoWithItems() {
        MedicalOrder order = new MedicalOrder();
        order.setOrderNo("MO-001");
        order.setPatientId(10L);
        order.setDoctorId(20L);
        order.setDiagnosis("感冒");
        order.setIsUrgent(true);

        MedicalOrderItem item = new MedicalOrderItem();
        item.setItemCode("D001");
        item.setItemName("阿莫西林");
        item.setSpecification("0.25g");
        item.setQuantity(new BigDecimal("2"));
        item.setUnit("盒");
        item.setDosage("1粒");
        item.setUsageMethod("口服");
        item.setFrequency("每日3次");
        item.setDays(3);

        MedicationOrderDTO dto = MedicalOrderConverter.toMedicationOrderDTO(
                order, List.of(item), "张三", "李医生");

        assertThat(dto.getOrderNo()).isEqualTo("MO-001");
        assertThat(dto.getPatientId()).isEqualTo(10L);
        assertThat(dto.getDoctorId()).isEqualTo(20L);
        assertThat(dto.getDiagnosis()).isEqualTo("感冒");
        assertThat(dto.getIsUrgent()).isTrue();
        assertThat(dto.getPatientName()).isEqualTo("张三");
        assertThat(dto.getDoctorName()).isEqualTo("李医生");
        assertThat(dto.getItems()).hasSize(1);
        MedicationOrderDTO.MedicationOrderItemDTO contractItem = dto.getItems().get(0);
        assertThat(contractItem.getItemCode()).isEqualTo("D001");
        assertThat(contractItem.getItemName()).isEqualTo("阿莫西林");
        assertThat(contractItem.getSpecification()).isEqualTo("0.25g");
        assertThat(contractItem.getQuantity()).isEqualByComparingTo("2");
        assertThat(contractItem.getUnit()).isEqualTo("盒");
        assertThat(contractItem.getDosage()).isEqualTo("1粒");
        assertThat(contractItem.getUsageMethod()).isEqualTo("口服");
        assertThat(contractItem.getFrequency()).isEqualTo("每日3次");
        assertThat(contractItem.getDays()).isEqualTo(3);
    }

    @Test
    @DisplayName("toMedicationOrderDTO 当明细为 null 时 items 也为 null")
    void shouldBuildMedicationOrderDtoWhenItemsNull() {
        MedicalOrder order = new MedicalOrder();
        order.setOrderNo("MO-002");

        MedicationOrderDTO dto = MedicalOrderConverter.toMedicationOrderDTO(
                order, null, null, null);

        assertThat(dto.getOrderNo()).isEqualTo("MO-002");
        assertThat(dto.getPatientName()).isNull();
        assertThat(dto.getDoctorName()).isNull();
        assertThat(dto.getItems()).isNull();
    }
}
