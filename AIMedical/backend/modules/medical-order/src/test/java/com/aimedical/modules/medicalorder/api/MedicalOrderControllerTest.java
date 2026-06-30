package com.aimedical.modules.medicalorder.api;

import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.common.result.Result;
import com.aimedical.modules.medicalorder.dto.ChargePreOrderDTO;
import com.aimedical.modules.medicalorder.dto.MedicalOrderCreateRequest;
import com.aimedical.modules.medicalorder.dto.MedicalOrderDTO;
import com.aimedical.modules.medicalorder.dto.MedicationOrderDTO;
import com.aimedical.modules.medicalorder.entity.ChargeStatus;
import com.aimedical.modules.medicalorder.entity.OrderStatus;
import com.aimedical.modules.medicalorder.entity.OrderType;
import com.aimedical.modules.medicalorder.service.MedicalOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MedicalOrderController 控制器")
class MedicalOrderControllerTest {

    @Mock
    private MedicalOrderService medicalOrderService;

    @InjectMocks
    private MedicalOrderController controller;

    private MedicalOrderDTO buildOrderDto() {
        MedicalOrderDTO dto = new MedicalOrderDTO();
        dto.setId(1L);
        dto.setOrderNo("MO-001");
        dto.setOrderStatus(OrderStatus.DRAFT);
        dto.setOrderType(OrderType.DRUG);
        dto.setTotalAmount(BigDecimal.ZERO);
        return dto;
    }

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("createOrder 应委托 service 并返回成功结果")
    void shouldCreateOrder() {
        MedicalOrderCreateRequest request = new MedicalOrderCreateRequest();
        request.setPatientId(10L);
        request.setDoctorId(20L);
        request.setRegistrationId(30L);
        request.setOrderType(OrderType.DRUG);
        MedicalOrderDTO dto = buildOrderDto();
        when(medicalOrderService.createOrder(request)).thenReturn(dto);

        Result<MedicalOrderDTO> result = controller.createOrder(request);

        assertThat(result.getCode()).isEqualTo(GlobalErrorCode.SUCCESS.getCode());
        assertThat(result.getData().getId()).isEqualTo(1L);
        verify(medicalOrderService).createOrder(request);
    }

    @Test
    @DisplayName("getOrder 应委托 service 并返回成功结果")
    void shouldGetOrder() {
        when(medicalOrderService.getOrder(1L)).thenReturn(buildOrderDto());

        Result<MedicalOrderDTO> result = controller.getOrder(1L);

        assertThat(result.getCode()).isEqualTo(GlobalErrorCode.SUCCESS.getCode());
        assertThat(result.getData().getOrderNo()).isEqualTo("MO-001");
        verify(medicalOrderService).getOrder(1L);
    }

    @Test
    @DisplayName("submitOrder 应委托 service")
    void shouldSubmitOrder() {
        MedicalOrderDTO dto = buildOrderDto();
        dto.setOrderStatus(OrderStatus.SUBMITTED);
        when(medicalOrderService.submitOrder(1L)).thenReturn(dto);

        Result<MedicalOrderDTO> result = controller.submitOrder(1L);

        assertThat(result.getData().getOrderStatus()).isEqualTo(OrderStatus.SUBMITTED);
        verify(medicalOrderService).submitOrder(1L);
    }

    @Test
    @DisplayName("cancelOrder 应委托 service")
    void shouldCancelOrder() {
        MedicalOrderDTO dto = buildOrderDto();
        dto.setOrderStatus(OrderStatus.CANCELLED);
        when(medicalOrderService.cancelOrder(1L)).thenReturn(dto);

        Result<MedicalOrderDTO> result = controller.cancelOrder(1L);

        assertThat(result.getData().getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(medicalOrderService).cancelOrder(1L);
    }

    @Test
    @DisplayName("chargeOrder 应委托 service")
    void shouldChargeOrder() {
        MedicalOrderDTO dto = buildOrderDto();
        dto.setOrderStatus(OrderStatus.CHARGED);
        when(medicalOrderService.chargeOrder(1L)).thenReturn(dto);

        Result<MedicalOrderDTO> result = controller.chargeOrder(1L);

        assertThat(result.getData().getOrderStatus()).isEqualTo(OrderStatus.CHARGED);
        verify(medicalOrderService).chargeOrder(1L);
    }

    @Test
    @DisplayName("dispenseOrder 应委托 service")
    void shouldDispenseOrder() {
        MedicalOrderDTO dto = buildOrderDto();
        dto.setOrderStatus(OrderStatus.DISPENSED);
        when(medicalOrderService.dispenseOrder(1L)).thenReturn(dto);

        Result<MedicalOrderDTO> result = controller.dispenseOrder(1L);

        assertThat(result.getData().getOrderStatus()).isEqualTo(OrderStatus.DISPENSED);
        verify(medicalOrderService).dispenseOrder(1L);
    }

    @Test
    @DisplayName("completeOrder 应委托 service")
    void shouldCompleteOrder() {
        MedicalOrderDTO dto = buildOrderDto();
        dto.setOrderStatus(OrderStatus.COMPLETED);
        when(medicalOrderService.completeOrder(1L)).thenReturn(dto);

        Result<MedicalOrderDTO> result = controller.completeOrder(1L);

        assertThat(result.getData().getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
        verify(medicalOrderService).completeOrder(1L);
    }

    @Test
    @DisplayName("generateChargePreOrder 应委托 service")
    void shouldGenerateChargePreOrder() {
        ChargePreOrderDTO dto = new ChargePreOrderDTO();
        dto.setId(500L);
        dto.setChargeStatus(ChargeStatus.PENDING);
        when(medicalOrderService.generateChargePreOrder(1L)).thenReturn(dto);

        Result<ChargePreOrderDTO> result = controller.generateChargePreOrder(1L);

        assertThat(result.getData().getId()).isEqualTo(500L);
        verify(medicalOrderService).generateChargePreOrder(1L);
    }

    @Test
    @DisplayName("getOrdersByPatient 应委托 service 并返回分页结果")
    void shouldGetOrdersByPatient() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<MedicalOrderDTO> page = new PageImpl<>(Collections.singletonList(buildOrderDto()));
        when(medicalOrderService.getOrdersByPatient(eq(10L), any(Pageable.class))).thenReturn(page);

        Result<Page<MedicalOrderDTO>> result = controller.getOrdersByPatient(10L, pageable);

        assertThat(result.getData().getContent()).hasSize(1);
        verify(medicalOrderService).getOrdersByPatient(10L, pageable);
    }

    @Test
    @DisplayName("getOrdersByDoctor 应委托 service 并返回分页结果")
    void shouldGetOrdersByDoctor() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<MedicalOrderDTO> page = new PageImpl<>(Collections.singletonList(buildOrderDto()));
        when(medicalOrderService.getOrdersByDoctor(eq(20L), any(Pageable.class))).thenReturn(page);

        Result<Page<MedicalOrderDTO>> result = controller.getOrdersByDoctor(20L, pageable);

        assertThat(result.getData().getContent()).hasSize(1);
        verify(medicalOrderService).getOrdersByDoctor(20L, pageable);
    }

    @Test
    @DisplayName("getMedicationOrderContract 应委托 service")
    void shouldGetMedicationOrderContract() {
        MedicationOrderDTO dto = new MedicationOrderDTO();
        dto.setOrderNo("MO-001");
        dto.setPatientName("张三");
        when(medicalOrderService.buildMedicationOrderContract(1L)).thenReturn(dto);

        Result<MedicationOrderDTO> result = controller.getMedicationOrderContract(1L);

        assertThat(result.getData().getOrderNo()).isEqualTo("MO-001");
        assertThat(result.getData().getPatientName()).isEqualTo("张三");
        verify(medicalOrderService).buildMedicationOrderContract(1L);
    }
}
