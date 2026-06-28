package com.aimedical.modules.medicalorder.service;

import com.aimedical.modules.medicalorder.dto.ChargePreOrderDTO;
import com.aimedical.modules.medicalorder.dto.MedicalOrderCreateRequest;
import com.aimedical.modules.medicalorder.dto.MedicalOrderDTO;
import com.aimedical.modules.medicalorder.dto.MedicationOrderDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MedicalOrderService {

    MedicalOrderDTO createOrder(MedicalOrderCreateRequest request);

    MedicalOrderDTO getOrder(Long id);

    MedicalOrderDTO submitOrder(Long id);

    MedicalOrderDTO cancelOrder(Long id);

    MedicalOrderDTO chargeOrder(Long id);

    MedicalOrderDTO dispenseOrder(Long id);

    MedicalOrderDTO completeOrder(Long id);

    ChargePreOrderDTO generateChargePreOrder(Long orderId);

    Page<MedicalOrderDTO> getOrdersByPatient(Long patientId, Pageable pageable);

    Page<MedicalOrderDTO> getOrdersByDoctor(Long doctorId, Pageable pageable);

    MedicationOrderDTO buildMedicationOrderContract(Long orderId);

}