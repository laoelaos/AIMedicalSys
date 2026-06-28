package com.aimedical.modules.medicalorder.repository;

import com.aimedical.modules.medicalorder.entity.MedicalOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicalOrderRepository extends JpaRepository<MedicalOrder, Long> {

    List<MedicalOrder> findByPatientId(Long patientId);

    List<MedicalOrder> findByDoctorId(Long doctorId);

    Optional<MedicalOrder> findByOrderNo(String orderNo);

    List<MedicalOrder> findByOrderStatus(String status);
}