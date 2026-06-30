package com.aimedical.modules.medicalorder.service.impl;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.modules.doctor.service.DoctorService;
import com.aimedical.modules.medicalorder.dto.ChargePreOrderDTO;
import com.aimedical.modules.medicalorder.dto.MedicalOrderCreateRequest;
import com.aimedical.modules.medicalorder.dto.MedicalOrderDTO;
import com.aimedical.modules.medicalorder.dto.MedicalOrderItemDTO;
import com.aimedical.modules.medicalorder.dto.MedicationOrderDTO;
import com.aimedical.modules.medicalorder.entity.ChargePreOrder;
import com.aimedical.modules.medicalorder.entity.ChargeStatus;
import com.aimedical.modules.medicalorder.entity.ItemType;
import com.aimedical.modules.medicalorder.entity.MedicalOrder;
import com.aimedical.modules.medicalorder.entity.MedicalOrderItem;
import com.aimedical.modules.medicalorder.entity.OrderStatus;
import com.aimedical.modules.medicalorder.entity.OrderType;
import com.aimedical.modules.medicalorder.repository.ChargePreOrderItemRepository;
import com.aimedical.modules.medicalorder.repository.ChargePreOrderRepository;
import com.aimedical.modules.medicalorder.repository.MedicalOrderItemRepository;
import com.aimedical.modules.medicalorder.repository.MedicalOrderRepository;
import com.aimedical.modules.patient.service.PatientService;
import com.aimedical.modules.registration.entity.Registration;
import com.aimedical.modules.registration.entity.RegistrationStatus;
import com.aimedical.modules.registration.repository.RegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalOrderServiceImplTest {

    @Mock private MedicalOrderRepository orderRepository;
    @Mock private MedicalOrderItemRepository itemRepository;
    @Mock private ChargePreOrderRepository chargePreOrderRepository;
    @Mock private ChargePreOrderItemRepository chargePreOrderItemRepository;
    @Mock private PatientService patientService;
    @Mock private DoctorService doctorService;
    @Mock private RegistrationRepository registrationRepository;

    @InjectMocks
    private MedicalOrderServiceImpl service;

    private static final Long PATIENT_ID = 10L;
    private static final Long DOCTOR_ID = 20L;
    private static final Long REGISTRATION_ID = 30L;
    private static final Long ORDER_ID = 100L;

    @BeforeEach
    void setUp() {
        // 默认为存在的患者/医生
    }

    // ============ 辅助方法 ============

    private MedicalOrderCreateRequest buildValidRequest() {
        MedicalOrderCreateRequest request = new MedicalOrderCreateRequest();
        request.setPatientId(PATIENT_ID);
        request.setDoctorId(DOCTOR_ID);
        request.setRegistrationId(REGISTRATION_ID);
        request.setOrderType(OrderType.DRUG);
        request.setDiagnosis("感冒");
        request.setIsUrgent(false);
        request.setRemark("测试");
        request.setItems(List.of(buildItemDTO(ItemType.DRUG, "D001", "阿莫西林", "2", "10")));
        return request;
    }

    private MedicalOrderItemDTO buildItemDTO(ItemType type, String code, String name, String qty, String price) {
        MedicalOrderItemDTO dto = new MedicalOrderItemDTO();
        dto.setItemType(type);
        dto.setItemCode(code);
        dto.setItemName(name);
        dto.setQuantity(new BigDecimal(qty));
        dto.setUnitPrice(new BigDecimal(price));
        dto.setUnit("盒");
        dto.setDosage("1粒");
        dto.setUsageMethod("口服");
        dto.setFrequency("每日3次");
        dto.setDays(3);
        return dto;
    }

    private Registration buildRegistration(RegistrationStatus status) {
        Registration reg = new Registration();
        reg.setPatientId(PATIENT_ID);
        reg.setStatus(status);
        return reg;
    }

    /** 模拟 save 时为订单分配 ID 并返回同一对象 */
    private void mockSaveAssignId() {
        when(orderRepository.save(any(MedicalOrder.class))).thenAnswer(inv -> {
            MedicalOrder o = inv.getArgument(0);
            if (o.getId() == null) {
                o.setId(ORDER_ID);
            }
            return o;
        });
    }

    private MedicalOrder buildOrder(OrderStatus status) {
        MedicalOrder order = new MedicalOrder();
        order.setId(ORDER_ID);
        order.setPatientId(PATIENT_ID);
        order.setDoctorId(DOCTOR_ID);
        order.setRegistrationId(REGISTRATION_ID);
        order.setOrderNo("MO-test-001");
        order.setOrderType(OrderType.DRUG);
        order.setOrderStatus(status);
        order.setDiagnosis("感冒");
        order.setTotalAmount(BigDecimal.ZERO);
        order.setIsUrgent(false);
        return order;
    }

    private MedicalOrderItem buildItemEntity(ItemType type, String code, String name, String qty, String price) {
        MedicalOrderItem item = new MedicalOrderItem();
        item.setId(1L);
        item.setOrderId(ORDER_ID);
        item.setItemType(type);
        item.setItemCode(code);
        item.setItemName(name);
        item.setQuantity(new BigDecimal(qty));
        item.setUnitPrice(new BigDecimal(price));
        item.setAmount(new BigDecimal(price).multiply(new BigDecimal(qty)));
        return item;
    }

    // ============ createOrder ============

    @Nested
    @DisplayName("createOrder 创建医嘱")
    class CreateOrder {

        @Test
        @DisplayName("正常创建带明细的草稿医嘱，应计算总金额并保存明细")
        void shouldCreateDraftOrderWithItems() {
            MedicalOrderCreateRequest request = buildValidRequest();
            request.setItems(List.of(
                    buildItemDTO(ItemType.DRUG, "D001", "阿莫西林", "2", "10"),
                    buildItemDTO(ItemType.EXAMINATION, "E001", "血常规", "1", "50")
            ));

            when(patientService.existsById(PATIENT_ID)).thenReturn(true);
            when(doctorService.existsById(DOCTOR_ID)).thenReturn(true);
            when(registrationRepository.findById(REGISTRATION_ID))
                    .thenReturn(Optional.of(buildRegistration(RegistrationStatus.CONFIRMED)));
            mockSaveAssignId();
            when(itemRepository.findByOrderId(ORDER_ID)).thenReturn(Collections.emptyList());

            MedicalOrderDTO result = service.createOrder(request);

            assertThat(result.getId()).isEqualTo(ORDER_ID);
            assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.DRAFT);
            assertThat(result.getOrderType()).isEqualTo(OrderType.DRUG);
            assertThat(result.getTotalAmount()).isEqualByComparingTo("70");
            assertThat(result.getOrderNo()).startsWith("MO-");
            verify(itemRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("创建时明细为 null，总金额应为 0 且不保存明细")
        void shouldCreateOrderWhenItemsNull() {
            MedicalOrderCreateRequest request = buildValidRequest();
            request.setItems(null);

            when(patientService.existsById(PATIENT_ID)).thenReturn(true);
            when(doctorService.existsById(DOCTOR_ID)).thenReturn(true);
            when(registrationRepository.findById(REGISTRATION_ID))
                    .thenReturn(Optional.of(buildRegistration(RegistrationStatus.PENDING)));
            mockSaveAssignId();
            when(itemRepository.findByOrderId(ORDER_ID)).thenReturn(Collections.emptyList());

            MedicalOrderDTO result = service.createOrder(request);

            assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            verify(itemRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("创建时明细为空列表，总金额应为 0 且不保存明细")
        void shouldCreateOrderWhenItemsEmpty() {
            MedicalOrderCreateRequest request = buildValidRequest();
            request.setItems(Collections.emptyList());

            when(patientService.existsById(PATIENT_ID)).thenReturn(true);
            when(doctorService.existsById(DOCTOR_ID)).thenReturn(true);
            when(registrationRepository.findById(REGISTRATION_ID))
                    .thenReturn(Optional.of(buildRegistration(RegistrationStatus.COMPLETED)));
            mockSaveAssignId();
            when(itemRepository.findByOrderId(ORDER_ID)).thenReturn(Collections.emptyList());

            MedicalOrderDTO result = service.createOrder(request);

            assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            verify(itemRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("明细数量/单价为 null 时，该明细金额按 0 计算")
        void shouldHandleNullPriceOrQuantityInItems() {
            MedicalOrderCreateRequest request = buildValidRequest();
            MedicalOrderItemDTO item = new MedicalOrderItemDTO();
            item.setItemType(ItemType.DRUG);
            item.setItemCode("D002");
            item.setItemName("维生素");
            item.setQuantity(null);
            item.setUnitPrice(null);
            request.setItems(List.of(item));

            when(patientService.existsById(PATIENT_ID)).thenReturn(true);
            when(doctorService.existsById(DOCTOR_ID)).thenReturn(true);
            when(registrationRepository.findById(REGISTRATION_ID))
                    .thenReturn(Optional.of(buildRegistration(RegistrationStatus.CONFIRMED)));
            mockSaveAssignId();
            when(itemRepository.findByOrderId(ORDER_ID)).thenReturn(Collections.emptyList());

            MedicalOrderDTO result = service.createOrder(request);

            assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("患者ID为空时抛出 PARAM_INVALID")
        void shouldThrowWhenPatientIdNull() {
            MedicalOrderCreateRequest request = buildValidRequest();
            request.setPatientId(null);

            assertThatThrownBy(() -> service.createOrder(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.PARAM_INVALID));
        }

        @Test
        @DisplayName("医生ID为空时抛出 PARAM_INVALID")
        void shouldThrowWhenDoctorIdNull() {
            MedicalOrderCreateRequest request = buildValidRequest();
            request.setDoctorId(null);

            assertThatThrownBy(() -> service.createOrder(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.PARAM_INVALID));
        }

        @Test
        @DisplayName("挂号ID为空时抛出 PARAM_INVALID")
        void shouldThrowWhenRegistrationIdNull() {
            MedicalOrderCreateRequest request = buildValidRequest();
            request.setRegistrationId(null);

            assertThatThrownBy(() -> service.createOrder(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.PARAM_INVALID));
        }

        @Test
        @DisplayName("患者不存在时抛出 NOT_FOUND")
        void shouldThrowWhenPatientNotExists() {
            MedicalOrderCreateRequest request = buildValidRequest();
            when(patientService.existsById(PATIENT_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.createOrder(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.NOT_FOUND));
        }

        @Test
        @DisplayName("医生不存在时抛出 NOT_FOUND")
        void shouldThrowWhenDoctorNotExists() {
            MedicalOrderCreateRequest request = buildValidRequest();
            when(patientService.existsById(PATIENT_ID)).thenReturn(true);
            when(doctorService.existsById(DOCTOR_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.createOrder(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.NOT_FOUND));
        }

        @Test
        @DisplayName("挂号记录不存在时抛出 NOT_FOUND")
        void shouldThrowWhenRegistrationNotFound() {
            MedicalOrderCreateRequest request = buildValidRequest();
            when(patientService.existsById(PATIENT_ID)).thenReturn(true);
            when(doctorService.existsById(DOCTOR_ID)).thenReturn(true);
            when(registrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createOrder(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.NOT_FOUND));
        }

        @Test
        @DisplayName("挂号记录与患者不匹配时抛出 PARAM_INVALID")
        void shouldThrowWhenRegistrationPatientMismatch() {
            MedicalOrderCreateRequest request = buildValidRequest();
            Registration reg = buildRegistration(RegistrationStatus.CONFIRMED);
            reg.setPatientId(999L);

            when(patientService.existsById(PATIENT_ID)).thenReturn(true);
            when(doctorService.existsById(DOCTOR_ID)).thenReturn(true);
            when(registrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(reg));

            assertThatThrownBy(() -> service.createOrder(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.PARAM_INVALID));
        }

        @Test
        @DisplayName("挂号状态为 CANCELLED 时抛出 REGISTRATION_STATUS_INVALID")
        void shouldThrowWhenRegistrationCancelled() {
            MedicalOrderCreateRequest request = buildValidRequest();
            when(patientService.existsById(PATIENT_ID)).thenReturn(true);
            when(doctorService.existsById(DOCTOR_ID)).thenReturn(true);
            when(registrationRepository.findById(REGISTRATION_ID))
                    .thenReturn(Optional.of(buildRegistration(RegistrationStatus.CANCELLED)));

            assertThatThrownBy(() -> service.createOrder(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.REGISTRATION_STATUS_INVALID));
        }

        @Test
        @DisplayName("挂号状态为 NO_SHOW 时抛出 REGISTRATION_STATUS_INVALID")
        void shouldThrowWhenRegistrationNoShow() {
            MedicalOrderCreateRequest request = buildValidRequest();
            when(patientService.existsById(PATIENT_ID)).thenReturn(true);
            when(doctorService.existsById(DOCTOR_ID)).thenReturn(true);
            when(registrationRepository.findById(REGISTRATION_ID))
                    .thenReturn(Optional.of(buildRegistration(RegistrationStatus.NO_SHOW)));

            assertThatThrownBy(() -> service.createOrder(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.REGISTRATION_STATUS_INVALID));
        }

        @Test
        @DisplayName("保存订单遇到 DataIntegrityViolationException 后重试成功")
        void shouldRetryOnDataIntegrityViolationThenSucceed() {
            MedicalOrderCreateRequest request = buildValidRequest();
            when(patientService.existsById(PATIENT_ID)).thenReturn(true);
            when(doctorService.existsById(DOCTOR_ID)).thenReturn(true);
            when(registrationRepository.findById(REGISTRATION_ID))
                    .thenReturn(Optional.of(buildRegistration(RegistrationStatus.CONFIRMED)));
            when(orderRepository.save(any(MedicalOrder.class)))
                    .thenThrow(new DataIntegrityViolationException("dup"))
                    .thenThrow(new DataIntegrityViolationException("dup"))
                    .thenAnswer(inv -> {
                        MedicalOrder o = inv.getArgument(0);
                        o.setId(ORDER_ID);
                        return o;
                    });
            when(itemRepository.findByOrderId(ORDER_ID)).thenReturn(Collections.emptyList());

            MedicalOrderDTO result = service.createOrder(request);

            assertThat(result.getId()).isEqualTo(ORDER_ID);
        }

        @Test
        @DisplayName("保存订单重试 3 次均失败时抛出 SYSTEM_ERROR")
        void shouldThrowSystemErrorWhenRetryExhausted() {
            MedicalOrderCreateRequest request = buildValidRequest();
            when(patientService.existsById(PATIENT_ID)).thenReturn(true);
            when(doctorService.existsById(DOCTOR_ID)).thenReturn(true);
            when(registrationRepository.findById(REGISTRATION_ID))
                    .thenReturn(Optional.of(buildRegistration(RegistrationStatus.CONFIRMED)));
            when(orderRepository.save(any(MedicalOrder.class)))
                    .thenThrow(new DataIntegrityViolationException("dup"));

            assertThatThrownBy(() -> service.createOrder(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.SYSTEM_ERROR));
        }
    }

    // ============ getOrder ============

    @Nested
    @DisplayName("getOrder 查询医嘱")
    class GetOrder {

        @Test
        @DisplayName("订单存在时返回 DTO（含明细）")
        void shouldReturnOrderWithItems() {
            MedicalOrder order = buildOrder(OrderStatus.DRAFT);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(itemRepository.findByOrderId(ORDER_ID))
                    .thenReturn(List.of(buildItemEntity(ItemType.DRUG, "D001", "阿莫西林", "2", "10")));

            MedicalOrderDTO result = service.getOrder(ORDER_ID);

            assertThat(result.getId()).isEqualTo(ORDER_ID);
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getItemName()).isEqualTo("阿莫西林");
        }

        @Test
        @DisplayName("订单不存在时抛出 NOT_FOUND")
        void shouldThrowWhenOrderNotFound() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getOrder(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.NOT_FOUND));
        }
    }

    // ============ submitOrder ============

    @Nested
    @DisplayName("submitOrder 提交医嘱")
    class SubmitOrder {

        @Test
        @DisplayName("草稿订单提交成功")
        void shouldSubmitDraftOrder() {
            MedicalOrder order = buildOrder(OrderStatus.DRAFT);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(itemRepository.findByOrderId(ORDER_ID))
                    .thenReturn(List.of(buildItemEntity(ItemType.DRUG, "D001", "阿莫西林", "2", "10")));
            when(orderRepository.save(any(MedicalOrder.class))).thenAnswer(inv -> inv.getArgument(0));

            MedicalOrderDTO result = service.submitOrder(ORDER_ID);

            assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.SUBMITTED);
            assertThat(result.getTotalAmount()).isEqualByComparingTo("20");
        }

        @Test
        @DisplayName("订单不存在时抛出 NOT_FOUND")
        void shouldThrowWhenNotFound() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.submitOrder(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.NOT_FOUND));
        }

        @Test
        @DisplayName("非草稿状态提交时抛出 ORDER_STATUS_INVALID")
        void shouldThrowWhenStatusNotDraft() {
            MedicalOrder order = buildOrder(OrderStatus.SUBMITTED);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.submitOrder(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.ORDER_STATUS_INVALID));
        }

        @Test
        @DisplayName("明细为空时抛出 ORDER_ITEM_EMPTY")
        void shouldThrowWhenItemsEmpty() {
            MedicalOrder order = buildOrder(OrderStatus.DRAFT);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(itemRepository.findByOrderId(ORDER_ID)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> service.submitOrder(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.ORDER_ITEM_EMPTY));
        }

        @Test
        @DisplayName("明细返回 null 时抛出 ORDER_ITEM_EMPTY")
        void shouldThrowWhenItemsNull() {
            MedicalOrder order = buildOrder(OrderStatus.DRAFT);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(itemRepository.findByOrderId(ORDER_ID)).thenReturn(null);

            assertThatThrownBy(() -> service.submitOrder(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.ORDER_ITEM_EMPTY));
        }

        @Test
        @DisplayName("明细 amount 为 null 时按 0 累加")
        void shouldTreatNullAmountAsZero() {
            MedicalOrder order = buildOrder(OrderStatus.DRAFT);
            MedicalOrderItem item = buildItemEntity(ItemType.DRUG, "D001", "阿莫西林", "2", "10");
            item.setAmount(null);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(itemRepository.findByOrderId(ORDER_ID)).thenReturn(List.of(item));
            when(orderRepository.save(any(MedicalOrder.class))).thenAnswer(inv -> inv.getArgument(0));

            MedicalOrderDTO result = service.submitOrder(ORDER_ID);

            assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("保存时发生乐观锁冲突抛出 SYSTEM_ERROR")
        void shouldThrowSystemErrorOnOptimisticLock() {
            MedicalOrder order = buildOrder(OrderStatus.DRAFT);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(itemRepository.findByOrderId(ORDER_ID))
                    .thenReturn(List.of(buildItemEntity(ItemType.DRUG, "D001", "阿莫西林", "2", "10")));
            when(orderRepository.save(any(MedicalOrder.class)))
                    .thenThrow(new OptimisticLockingFailureException("version conflict"));

            assertThatThrownBy(() -> service.submitOrder(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.SYSTEM_ERROR));
        }
    }

    // ============ cancelOrder ============

    @Nested
    @DisplayName("cancelOrder 取消医嘱")
    class CancelOrder {

        @Test
        @DisplayName("草稿订单取消成功")
        void shouldCancelDraftOrder() {
            MedicalOrder order = buildOrder(OrderStatus.DRAFT);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(MedicalOrder.class))).thenAnswer(inv -> inv.getArgument(0));
            when(itemRepository.findByOrderId(ORDER_ID)).thenReturn(Collections.emptyList());

            MedicalOrderDTO result = service.cancelOrder(ORDER_ID);

            assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("已提交订单取消成功")
        void shouldCancelSubmittedOrder() {
            MedicalOrder order = buildOrder(OrderStatus.SUBMITTED);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(MedicalOrder.class))).thenAnswer(inv -> inv.getArgument(0));
            when(itemRepository.findByOrderId(ORDER_ID)).thenReturn(Collections.emptyList());

            MedicalOrderDTO result = service.cancelOrder(ORDER_ID);

            assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("订单不存在时抛出 NOT_FOUND")
        void shouldThrowWhenNotFound() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cancelOrder(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.NOT_FOUND));
        }

        @Test
        @DisplayName("已收费状态取消时抛出 ORDER_STATUS_INVALID")
        void shouldThrowWhenStatusInvalid() {
            MedicalOrder order = buildOrder(OrderStatus.CHARGED);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.cancelOrder(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.ORDER_STATUS_INVALID));
        }

        @Test
        @DisplayName("已完成状态取消时抛出 ORDER_STATUS_INVALID")
        void shouldThrowWhenCompleted() {
            MedicalOrder order = buildOrder(OrderStatus.COMPLETED);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.cancelOrder(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.ORDER_STATUS_INVALID));
        }
    }

    // ============ chargeOrder ============

    @Nested
    @DisplayName("chargeOrder 收费")
    class ChargeOrder {

        @Test
        @DisplayName("已提交订单收费成功，并同步更新收费前置单状态")
        void shouldChargeSubmittedOrderAndSyncPreOrder() {
            MedicalOrder order = buildOrder(OrderStatus.SUBMITTED);
            ChargePreOrder preOrder = new ChargePreOrder();
            preOrder.setId(200L);
            preOrder.setOrderId(ORDER_ID);
            preOrder.setChargeStatus(ChargeStatus.PENDING);

            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(MedicalOrder.class))).thenAnswer(inv -> inv.getArgument(0));
            when(chargePreOrderRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(preOrder));
            when(itemRepository.findByOrderId(ORDER_ID)).thenReturn(Collections.emptyList());

            MedicalOrderDTO result = service.chargeOrder(ORDER_ID);

            assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.CHARGED);
            assertThat(preOrder.getChargeStatus()).isEqualTo(ChargeStatus.CHARGED);
            verify(chargePreOrderRepository).save(preOrder);
        }

        @Test
        @DisplayName("无收费前置单时收费成功且不更新前置单")
        void shouldChargeWithoutPreOrder() {
            MedicalOrder order = buildOrder(OrderStatus.SUBMITTED);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(MedicalOrder.class))).thenAnswer(inv -> inv.getArgument(0));
            when(chargePreOrderRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(itemRepository.findByOrderId(ORDER_ID)).thenReturn(Collections.emptyList());

            MedicalOrderDTO result = service.chargeOrder(ORDER_ID);

            assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.CHARGED);
            verify(chargePreOrderRepository, never()).save(any(ChargePreOrder.class));
        }

        @Test
        @DisplayName("订单不存在时抛出 NOT_FOUND")
        void shouldThrowWhenNotFound() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.chargeOrder(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.NOT_FOUND));
        }

        @Test
        @DisplayName("非已提交状态收费时抛出 ORDER_STATUS_INVALID")
        void shouldThrowWhenStatusNotSubmitted() {
            MedicalOrder order = buildOrder(OrderStatus.DRAFT);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.chargeOrder(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.ORDER_STATUS_INVALID));
        }
    }

    // ============ dispenseOrder ============

    @Nested
    @DisplayName("dispenseOrder 发药")
    class DispenseOrder {

        @Test
        @DisplayName("已收费订单发药成功")
        void shouldDispenseChargedOrder() {
            MedicalOrder order = buildOrder(OrderStatus.CHARGED);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(MedicalOrder.class))).thenAnswer(inv -> inv.getArgument(0));
            when(itemRepository.findByOrderId(ORDER_ID)).thenReturn(Collections.emptyList());

            MedicalOrderDTO result = service.dispenseOrder(ORDER_ID);

            assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.DISPENSED);
        }

        @Test
        @DisplayName("订单不存在时抛出 NOT_FOUND")
        void shouldThrowWhenNotFound() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.dispenseOrder(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.NOT_FOUND));
        }

        @Test
        @DisplayName("非已收费状态发药时抛出 ORDER_STATUS_INVALID")
        void shouldThrowWhenStatusNotCharged() {
            MedicalOrder order = buildOrder(OrderStatus.SUBMITTED);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.dispenseOrder(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.ORDER_STATUS_INVALID));
        }
    }

    // ============ completeOrder ============

    @Nested
    @DisplayName("completeOrder 完成")
    class CompleteOrder {

        @Test
        @DisplayName("已收费订单完成成功")
        void shouldCompleteChargedOrder() {
            MedicalOrder order = buildOrder(OrderStatus.CHARGED);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(MedicalOrder.class))).thenAnswer(inv -> inv.getArgument(0));
            when(itemRepository.findByOrderId(ORDER_ID)).thenReturn(Collections.emptyList());

            MedicalOrderDTO result = service.completeOrder(ORDER_ID);

            assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
        }

        @Test
        @DisplayName("已发药订单完成成功")
        void shouldCompleteDispensedOrder() {
            MedicalOrder order = buildOrder(OrderStatus.DISPENSED);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(MedicalOrder.class))).thenAnswer(inv -> inv.getArgument(0));
            when(itemRepository.findByOrderId(ORDER_ID)).thenReturn(Collections.emptyList());

            MedicalOrderDTO result = service.completeOrder(ORDER_ID);

            assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
        }

        @Test
        @DisplayName("订单不存在时抛出 NOT_FOUND")
        void shouldThrowWhenNotFound() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.completeOrder(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.NOT_FOUND));
        }

        @Test
        @DisplayName("草稿状态完成时抛出 ORDER_STATUS_INVALID")
        void shouldThrowWhenStatusInvalid() {
            MedicalOrder order = buildOrder(OrderStatus.DRAFT);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.completeOrder(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.ORDER_STATUS_INVALID));
        }
    }

    // ============ generateChargePreOrder ============

    @Nested
    @DisplayName("generateChargePreOrder 生成收费前置单")
    class GenerateChargePreOrder {

        @Test
        @DisplayName("正常生成收费前置单（含多种项目类型转换）")
        void shouldGenerateChargePreOrder() {
            MedicalOrder order = buildOrder(OrderStatus.SUBMITTED);
            List<MedicalOrderItem> items = List.of(
                    buildItemEntity(ItemType.DRUG, "D001", "阿莫西林", "2", "10"),
                    buildItemEntity(ItemType.EXAMINATION, "E001", "血常规", "1", "50"),
                    buildItemEntity(ItemType.LAB_TEST, "L001", "尿常规", "1", "30")
            );

            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(itemRepository.findByOrderId(ORDER_ID)).thenReturn(items);
            when(chargePreOrderRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(chargePreOrderRepository.save(any(ChargePreOrder.class))).thenAnswer(inv -> {
                ChargePreOrder cpo = inv.getArgument(0);
                cpo.setId(500L);
                return cpo;
            });
            when(chargePreOrderItemRepository.findByChargePreOrderId(500L))
                    .thenReturn(List.of());

            ChargePreOrderDTO result = service.generateChargePreOrder(ORDER_ID);

            assertThat(result.getOrderId()).isEqualTo(ORDER_ID);
            assertThat(result.getChargeStatus()).isEqualTo(ChargeStatus.PENDING);
            assertThat(result.getTotalAmount()).isEqualByComparingTo("100");
            assertThat(result.getChargeNo()).startsWith("CP-");
            verify(chargePreOrderItemRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("项目类型为 null 时转换为 OTHER")
        void shouldConvertNullItemTypeToOther() {
            MedicalOrder order = buildOrder(OrderStatus.SUBMITTED);
            MedicalOrderItem item = buildItemEntity(ItemType.DRUG, "D001", "阿莫西林", "2", "10");
            item.setItemType(null);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(itemRepository.findByOrderId(ORDER_ID)).thenReturn(List.of(item));
            when(chargePreOrderRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(chargePreOrderRepository.save(any(ChargePreOrder.class))).thenAnswer(inv -> {
                ChargePreOrder cpo = inv.getArgument(0);
                cpo.setId(500L);
                return cpo;
            });
            when(chargePreOrderItemRepository.findByChargePreOrderId(500L))
                    .thenReturn(List.of());

            ChargePreOrderDTO result = service.generateChargePreOrder(ORDER_ID);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("明细 amount 为 null 时按 0 累加")
        void shouldTreatNullAmountAsZeroInTotal() {
            MedicalOrder order = buildOrder(OrderStatus.SUBMITTED);
            MedicalOrderItem item = buildItemEntity(ItemType.DRUG, "D001", "阿莫西林", "2", "10");
            item.setAmount(null);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(itemRepository.findByOrderId(ORDER_ID)).thenReturn(List.of(item));
            when(chargePreOrderRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(chargePreOrderRepository.save(any(ChargePreOrder.class))).thenAnswer(inv -> {
                ChargePreOrder cpo = inv.getArgument(0);
                cpo.setId(500L);
                return cpo;
            });
            when(chargePreOrderItemRepository.findByChargePreOrderId(500L))
                    .thenReturn(List.of());

            ChargePreOrderDTO result = service.generateChargePreOrder(ORDER_ID);

            assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("订单不存在时抛出 NOT_FOUND")
        void shouldThrowWhenNotFound() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.generateChargePreOrder(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.NOT_FOUND));
        }

        @Test
        @DisplayName("非已提交状态时抛出 ORDER_STATUS_INVALID")
        void shouldThrowWhenStatusNotSubmitted() {
            MedicalOrder order = buildOrder(OrderStatus.DRAFT);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.generateChargePreOrder(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.ORDER_STATUS_INVALID));
        }

        @Test
        @DisplayName("明细为空时抛出 ORDER_ITEM_EMPTY")
        void shouldThrowWhenItemsEmpty() {
            MedicalOrder order = buildOrder(OrderStatus.SUBMITTED);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(itemRepository.findByOrderId(ORDER_ID)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> service.generateChargePreOrder(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.ORDER_ITEM_EMPTY));
        }

        @Test
        @DisplayName("明细返回 null 时抛出 ORDER_ITEM_EMPTY")
        void shouldThrowWhenItemsNull() {
            MedicalOrder order = buildOrder(OrderStatus.SUBMITTED);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(itemRepository.findByOrderId(ORDER_ID)).thenReturn(null);

            assertThatThrownBy(() -> service.generateChargePreOrder(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.ORDER_ITEM_EMPTY));
        }

        @Test
        @DisplayName("收费前置单已存在时抛出 CHARGE_PRE_ORDER_EXISTS")
        void shouldThrowWhenPreOrderExists() {
            MedicalOrder order = buildOrder(OrderStatus.SUBMITTED);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(itemRepository.findByOrderId(ORDER_ID))
                    .thenReturn(List.of(buildItemEntity(ItemType.DRUG, "D001", "阿莫西林", "2", "10")));
            when(chargePreOrderRepository.findByOrderId(ORDER_ID))
                    .thenReturn(Optional.of(new ChargePreOrder()));

            assertThatThrownBy(() -> service.generateChargePreOrder(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.CHARGE_PRE_ORDER_EXISTS));
        }

        @Test
        @DisplayName("保存前置单时唯一约束冲突抛出 CHARGE_PRE_ORDER_EXISTS")
        void shouldThrowWhenSaveDataIntegrityViolation() {
            MedicalOrder order = buildOrder(OrderStatus.SUBMITTED);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(itemRepository.findByOrderId(ORDER_ID))
                    .thenReturn(List.of(buildItemEntity(ItemType.DRUG, "D001", "阿莫西林", "2", "10")));
            when(chargePreOrderRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(chargePreOrderRepository.save(any(ChargePreOrder.class)))
                    .thenThrow(new DataIntegrityViolationException("unique constraint"));

            assertThatThrownBy(() -> service.generateChargePreOrder(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.CHARGE_PRE_ORDER_EXISTS));
        }
    }

    // ============ getOrdersByPatient / getOrdersByDoctor ============

    @Nested
    @DisplayName("分页查询医嘱")
    class PageQueries {

        @Test
        @DisplayName("按患者查询返回带明细的分页结果")
        void shouldReturnPageByPatient() {
            Pageable pageable = PageRequest.of(0, 10);
            MedicalOrder order = buildOrder(OrderStatus.SUBMITTED);
            Page<MedicalOrder> page = new PageImpl<>(List.of(order), pageable, 1);
            when(orderRepository.findByPatientId(PATIENT_ID, pageable)).thenReturn(page);
            when(itemRepository.findByOrderIdIn(List.of(ORDER_ID)))
                    .thenReturn(List.of(buildItemEntity(ItemType.DRUG, "D001", "阿莫西林", "2", "10")));

            Page<MedicalOrderDTO> result = service.getOrdersByPatient(PATIENT_ID, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getItems()).hasSize(1);
        }

        @Test
        @DisplayName("按患者查询结果为空页")
        void shouldReturnEmptyPageByPatient() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<MedicalOrder> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            when(orderRepository.findByPatientId(PATIENT_ID, pageable)).thenReturn(emptyPage);

            Page<MedicalOrderDTO> result = service.getOrdersByPatient(PATIENT_ID, pageable);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("按医生查询返回带明细的分页结果")
        void shouldReturnPageByDoctor() {
            Pageable pageable = PageRequest.of(0, 10);
            MedicalOrder order = buildOrder(OrderStatus.SUBMITTED);
            Page<MedicalOrder> page = new PageImpl<>(List.of(order), pageable, 1);
            when(orderRepository.findByDoctorId(DOCTOR_ID, pageable)).thenReturn(page);
            when(itemRepository.findByOrderIdIn(List.of(ORDER_ID)))
                    .thenReturn(Collections.emptyList());

            Page<MedicalOrderDTO> result = service.getOrdersByDoctor(DOCTOR_ID, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getItems()).isEmpty();
        }

        @Test
        @DisplayName("按医生查询结果为空页")
        void shouldReturnEmptyPageByDoctor() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<MedicalOrder> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            when(orderRepository.findByDoctorId(DOCTOR_ID, pageable)).thenReturn(emptyPage);

            Page<MedicalOrderDTO> result = service.getOrdersByDoctor(DOCTOR_ID, pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    // ============ buildMedicationOrderContract ============

    @Nested
    @DisplayName("buildMedicationOrderContract 构建处方契约")
    class BuildMedicationOrderContract {

        @Test
        @DisplayName("只筛选药品类型明细，并填充患者/医生姓名")
        void shouldFilterDrugItemsOnly() {
            MedicalOrder order = buildOrder(OrderStatus.SUBMITTED);
            order.setOrderNo("MO-contract-001");
            List<MedicalOrderItem> items = List.of(
                    buildItemEntity(ItemType.DRUG, "D001", "阿莫西林", "2", "10"),
                    buildItemEntity(ItemType.EXAMINATION, "E001", "血常规", "1", "50"),
                    buildItemEntity(ItemType.LAB_TEST, "L001", "尿常规", "1", "30")
            );

            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(itemRepository.findByOrderId(ORDER_ID)).thenReturn(items);
            when(patientService.getRealName(PATIENT_ID)).thenReturn("张三");
            when(doctorService.getRealName(DOCTOR_ID)).thenReturn("李医生");

            MedicationOrderDTO result = service.buildMedicationOrderContract(ORDER_ID);

            assertThat(result.getOrderNo()).isEqualTo("MO-contract-001");
            assertThat(result.getPatientName()).isEqualTo("张三");
            assertThat(result.getDoctorName()).isEqualTo("李医生");
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getItemCode()).isEqualTo("D001");
        }

        @Test
        @DisplayName("无药品明细时返回空 items 列表")
        void shouldReturnEmptyItemsWhenNoDrug() {
            MedicalOrder order = buildOrder(OrderStatus.SUBMITTED);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(itemRepository.findByOrderId(ORDER_ID))
                    .thenReturn(List.of(buildItemEntity(ItemType.EXAMINATION, "E001", "血常规", "1", "50")));
            when(patientService.getRealName(PATIENT_ID)).thenReturn("张三");
            when(doctorService.getRealName(DOCTOR_ID)).thenReturn("李医生");

            MedicationOrderDTO result = service.buildMedicationOrderContract(ORDER_ID);

            assertThat(result.getItems()).isEmpty();
        }

        @Test
        @DisplayName("订单不存在时抛出 NOT_FOUND")
        void shouldThrowWhenNotFound() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buildMedicationOrderContract(ORDER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(GlobalErrorCode.NOT_FOUND));
        }

        @Test
        @DisplayName("患者ID为 null 时不查询患者姓名")
        void shouldHandleNullPatientId() {
            MedicalOrder order = buildOrder(OrderStatus.SUBMITTED);
            order.setPatientId(null);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(itemRepository.findByOrderId(ORDER_ID)).thenReturn(Collections.emptyList());
            when(doctorService.getRealName(DOCTOR_ID)).thenReturn("李医生");

            MedicationOrderDTO result = service.buildMedicationOrderContract(ORDER_ID);

            assertThat(result.getPatientName()).isNull();
            assertThat(result.getDoctorName()).isEqualTo("李医生");
            verify(patientService, never()).getRealName(any());
        }

        @Test
        @DisplayName("医生ID为 null 时不查询医生姓名")
        void shouldHandleNullDoctorId() {
            MedicalOrder order = buildOrder(OrderStatus.SUBMITTED);
            order.setDoctorId(null);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(itemRepository.findByOrderId(ORDER_ID)).thenReturn(Collections.emptyList());
            when(patientService.getRealName(PATIENT_ID)).thenReturn("张三");

            MedicationOrderDTO result = service.buildMedicationOrderContract(ORDER_ID);

            assertThat(result.getPatientName()).isEqualTo("张三");
            assertThat(result.getDoctorName()).isNull();
            verify(doctorService, never()).getRealName(any());
        }
    }
}
