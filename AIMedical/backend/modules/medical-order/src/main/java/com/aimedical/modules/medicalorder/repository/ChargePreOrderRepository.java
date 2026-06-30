package com.aimedical.modules.medicalorder.repository;

import com.aimedical.modules.medicalorder.entity.ChargePreOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChargePreOrderRepository extends JpaRepository<ChargePreOrder, Long> {

    Optional<ChargePreOrder> findByOrderId(Long orderId);

    Optional<ChargePreOrder> findByChargeNo(String chargeNo);
}