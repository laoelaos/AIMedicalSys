package com.aimedical.modules.medicalorder.repository;

import com.aimedical.modules.medicalorder.entity.ChargePreOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChargePreOrderItemRepository extends JpaRepository<ChargePreOrderItem, Long> {

    List<ChargePreOrderItem> findByChargePreOrderId(Long chargePreOrderId);
}