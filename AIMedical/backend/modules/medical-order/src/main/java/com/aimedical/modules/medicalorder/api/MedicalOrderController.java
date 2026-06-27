package com.aimedical.modules.medicalorder.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.medicalorder.dto.ChargePreOrderDTO;
import com.aimedical.modules.medicalorder.dto.MedicalOrderDTO;
import com.aimedical.modules.medicalorder.dto.MedicationOrderDTO;
import com.aimedical.modules.medicalorder.service.MedicalOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/medical-order")
public class MedicalOrderController {

    private final MedicalOrderService medicalOrderService;

    public MedicalOrderController(MedicalOrderService medicalOrderService) {
        this.medicalOrderService = medicalOrderService;
    }

    @PostMapping
    public Result<MedicalOrderDTO> createOrder(@RequestBody MedicalOrderDTO dto) {
        return Result.success(medicalOrderService.createOrder(dto));
    }

    @GetMapping("/{id}")
    public Result<MedicalOrderDTO> getOrder(@PathVariable Long id) {
        return Result.success(medicalOrderService.getOrder(id));
    }

    @PutMapping("/{id}/submit")
    public Result<MedicalOrderDTO> submitOrder(@PathVariable Long id) {
        return Result.success(medicalOrderService.submitOrder(id));
    }

    @PutMapping("/{id}/cancel")
    public Result<MedicalOrderDTO> cancelOrder(@PathVariable Long id) {
        return Result.success(medicalOrderService.cancelOrder(id));
    }

    @PostMapping("/{id}/charge")
    public Result<ChargePreOrderDTO> generateChargePreOrder(@PathVariable Long id) {
        return Result.success(medicalOrderService.generateChargePreOrder(id));
    }

    @GetMapping("/patient/{patientId}")
    public Result<List<MedicalOrderDTO>> getOrdersByPatient(@PathVariable Long patientId) {
        return Result.success(medicalOrderService.getOrdersByPatient(patientId));
    }

    @GetMapping("/doctor/{doctorId}")
    public Result<List<MedicalOrderDTO>> getOrdersByDoctor(@PathVariable Long doctorId) {
        return Result.success(medicalOrderService.getOrdersByDoctor(doctorId));
    }

    @GetMapping("/{id}/medication-contract")
    public Result<MedicationOrderDTO> getMedicationOrderContract(@PathVariable Long id) {
        return Result.success(medicalOrderService.buildMedicationOrderContract(id));
    }

}