package com.aimedical.modules.medicalorder.repository;

import com.aimedical.modules.medicalorder.entity.MedicalOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalOrderItemRepository extends JpaRepository<MedicalOrderItem, Long> {

    List<MedicalOrderItem> findByOrderId(Long orderId);
}