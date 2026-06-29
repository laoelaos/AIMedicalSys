package com.aimedical.modules.medicalorder.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("医嘱模块枚举类")
class MedicalOrderEnumsTest {

    @Test
    @DisplayName("OrderType 各枚举值的 code/desc 正确")
    void orderTypeShouldHaveCorrectCodeAndDesc() {
        assertThat(OrderType.DRUG.getCode()).isEqualTo("DRUG");
        assertThat(OrderType.DRUG.getDesc()).isEqualTo("药品");
        assertThat(OrderType.EXAMINATION.getCode()).isEqualTo("EXAMINATION");
        assertThat(OrderType.EXAMINATION.getDesc()).isEqualTo("检查");
        assertThat(OrderType.LAB_TEST.getCode()).isEqualTo("LAB_TEST");
        assertThat(OrderType.LAB_TEST.getDesc()).isEqualTo("检验");
        assertThat(OrderType.values()).hasSize(3);
    }

    @Test
    @DisplayName("ItemType 各枚举值的 code/desc 正确")
    void itemTypeShouldHaveCorrectCodeAndDesc() {
        assertThat(ItemType.DRUG.getCode()).isEqualTo("DRUG");
        assertThat(ItemType.DRUG.getDesc()).isEqualTo("药品");
        assertThat(ItemType.EXAMINATION.getCode()).isEqualTo("EXAMINATION");
        assertThat(ItemType.EXAMINATION.getDesc()).isEqualTo("检查");
        assertThat(ItemType.LAB_TEST.getCode()).isEqualTo("LAB_TEST");
        assertThat(ItemType.LAB_TEST.getDesc()).isEqualTo("检验");
        assertThat(ItemType.values()).hasSize(3);
    }

    @Test
    @DisplayName("OrderStatus 各枚举值的 code/desc 正确")
    void orderStatusShouldHaveCorrectCodeAndDesc() {
        assertThat(OrderStatus.DRAFT.getCode()).isEqualTo("DRAFT");
        assertThat(OrderStatus.DRAFT.getDesc()).isEqualTo("草稿");
        assertThat(OrderStatus.SUBMITTED.getCode()).isEqualTo("SUBMITTED");
        assertThat(OrderStatus.SUBMITTED.getDesc()).isEqualTo("已提交");
        assertThat(OrderStatus.CHARGED.getCode()).isEqualTo("CHARGED");
        assertThat(OrderStatus.CHARGED.getDesc()).isEqualTo("已收费");
        assertThat(OrderStatus.DISPENSED.getCode()).isEqualTo("DISPENSED");
        assertThat(OrderStatus.DISPENSED.getDesc()).isEqualTo("已发药");
        assertThat(OrderStatus.COMPLETED.getCode()).isEqualTo("COMPLETED");
        assertThat(OrderStatus.COMPLETED.getDesc()).isEqualTo("已完成");
        assertThat(OrderStatus.CANCELLED.getCode()).isEqualTo("CANCELLED");
        assertThat(OrderStatus.CANCELLED.getDesc()).isEqualTo("已取消");
        assertThat(OrderStatus.values()).hasSize(6);
    }

    @Test
    @DisplayName("ChargeStatus 各枚举值的 code/desc 正确")
    void chargeStatusShouldHaveCorrectCodeAndDesc() {
        assertThat(ChargeStatus.PENDING.getCode()).isEqualTo("PENDING");
        assertThat(ChargeStatus.PENDING.getDesc()).isEqualTo("待收费");
        assertThat(ChargeStatus.CHARGED.getCode()).isEqualTo("CHARGED");
        assertThat(ChargeStatus.CHARGED.getDesc()).isEqualTo("已收费");
        assertThat(ChargeStatus.REFUNDED.getCode()).isEqualTo("REFUNDED");
        assertThat(ChargeStatus.REFUNDED.getDesc()).isEqualTo("已退费");
        assertThat(ChargeStatus.values()).hasSize(3);
    }

    @Test
    @DisplayName("ChargeItemType 各枚举值的 code/desc 正确")
    void chargeItemTypeShouldHaveCorrectCodeAndDesc() {
        assertThat(ChargeItemType.DRUG.getCode()).isEqualTo("DRUG");
        assertThat(ChargeItemType.DRUG.getDesc()).isEqualTo("药品");
        assertThat(ChargeItemType.EXAMINATION.getCode()).isEqualTo("EXAMINATION");
        assertThat(ChargeItemType.EXAMINATION.getDesc()).isEqualTo("检查");
        assertThat(ChargeItemType.LAB_TEST.getCode()).isEqualTo("LAB_TEST");
        assertThat(ChargeItemType.LAB_TEST.getDesc()).isEqualTo("检验");
        assertThat(ChargeItemType.MATERIAL.getCode()).isEqualTo("MATERIAL");
        assertThat(ChargeItemType.MATERIAL.getDesc()).isEqualTo("材料");
        assertThat(ChargeItemType.OTHER.getCode()).isEqualTo("OTHER");
        assertThat(ChargeItemType.OTHER.getDesc()).isEqualTo("其他");
        assertThat(ChargeItemType.values()).hasSize(5);
    }

    @Test
    @DisplayName("OrderType 可通过名称获取枚举值")
    void orderTypeValueOf() {
        assertThat(OrderType.valueOf("DRUG")).isEqualTo(OrderType.DRUG);
    }

    @Test
    @DisplayName("ChargeItemType 可通过名称获取枚举值")
    void chargeItemTypeValueOf() {
        assertThat(ChargeItemType.valueOf("OTHER")).isEqualTo(ChargeItemType.OTHER);
    }
}
