package com.aimedical.modules.medicalorder.service;

import com.aimedical.modules.medicalorder.dto.ChargePreOrderDTO;
import com.aimedical.modules.medicalorder.dto.MedicalOrderDTO;
import com.aimedical.modules.medicalorder.dto.MedicationOrderDTO;

import java.util.List;

public interface MedicalOrderService {

    MedicalOrderDTO createOrder(MedicalOrderDTO dto);

    MedicalOrderDTO getOrder(Long id);

    MedicalOrderDTO submitOrder(Long id);

    MedicalOrderDTO cancelOrder(Long id);

    ChargePreOrderDTO generateChargePreOrder(Long orderId);

    List<MedicalOrderDTO> getOrdersByPatient(Long patientId);

    List<MedicalOrderDTO> getOrdersByDoctor(Long doctorId);

    MedicationOrderDTO buildMedicationOrderContract(Long orderId);

}