package com.aimedical.modules.medicalorder.repository;

import com.aimedical.modules.medicalorder.entity.MedicalOrder;
import com.aimedical.modules.medicalorder.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicalOrderRepository extends JpaRepository<MedicalOrder, Long> {

    Page<MedicalOrder> findByPatientId(Long patientId, Pageable pageable);

    Page<MedicalOrder> findByDoctorId(Long doctorId, Pageable pageable);

    Optional<MedicalOrder> findByOrderNo(String orderNo);

    List<MedicalOrder> findByOrderStatus(OrderStatus status);
}
