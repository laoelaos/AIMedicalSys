package com.aimedical.modules.medicalorder.api;

import com.aimedical.common.result.Result;
import com.aimedical.modules.medicalorder.dto.ChargePreOrderDTO;
import com.aimedical.modules.medicalorder.dto.MedicalOrderDTO;
import com.aimedical.modules.medicalorder.dto.MedicationOrderDTO;
import com.aimedical.modules.medicalorder.service.MedicalOrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/medical-orders")
public class MedicalOrderController {

    private final MedicalOrderService medicalOrderService;

    public MedicalOrderController(MedicalOrderService medicalOrderService) {
        this.medicalOrderService = medicalOrderService;
    }

    @PostMapping
    public Result<MedicalOrderDTO> createOrder(@Valid @RequestBody MedicalOrderDTO dto) {
        return Result.success(medicalOrderService.createOrder(dto));
    }

    @GetMapping("/{id}")
    public Result<MedicalOrderDTO> getOrder(@PathVariable Long id) {
        return Result.success(medicalOrderService.getOrder(id));
    }

    @PostMapping("/{id}/submit")
    public Result<MedicalOrderDTO> submitOrder(@PathVariable Long id) {
        return Result.success(medicalOrderService.submitOrder(id));
    }

    @PostMapping("/{id}/cancel")
    public Result<MedicalOrderDTO> cancelOrder(@PathVariable Long id) {
        return Result.success(medicalOrderService.cancelOrder(id));
    }

    @PostMapping("/{id}/charge")
    public Result<MedicalOrderDTO> chargeOrder(@PathVariable Long id) {
        return Result.success(medicalOrderService.chargeOrder(id));
    }

    @PostMapping("/{id}/dispense")
    public Result<MedicalOrderDTO> dispenseOrder(@PathVariable Long id) {
        return Result.success(medicalOrderService.dispenseOrder(id));
    }

    @PostMapping("/{id}/complete")
    public Result<MedicalOrderDTO> completeOrder(@PathVariable Long id) {
        return Result.success(medicalOrderService.completeOrder(id));
    }

    @PostMapping("/{id}/charge-pre")
    public Result<ChargePreOrderDTO> generateChargePreOrder(@PathVariable Long id) {
        return Result.success(medicalOrderService.generateChargePreOrder(id));
    }

    @GetMapping("/patient/{patientId}")
    public Result<Page<MedicalOrderDTO>> getOrdersByPatient(@PathVariable Long patientId, Pageable pageable) {
        return Result.success(medicalOrderService.getOrdersByPatient(patientId, pageable));
    }

    @GetMapping("/doctor/{doctorId}")
    public Result<Page<MedicalOrderDTO>> getOrdersByDoctor(@PathVariable Long doctorId, Pageable pageable) {
        return Result.success(medicalOrderService.getOrdersByDoctor(doctorId, pageable));
    }

    @GetMapping("/{id}/medication-contract")
    public Result<MedicationOrderDTO> getMedicationOrderContract(@PathVariable Long id) {
        return Result.success(medicalOrderService.buildMedicationOrderContract(id));
    }

}