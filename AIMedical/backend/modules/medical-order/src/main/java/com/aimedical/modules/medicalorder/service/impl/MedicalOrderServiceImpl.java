package com.aimedical.modules.medicalorder.service.impl;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.modules.doctor.repository.DoctorRepository;
import com.aimedical.modules.medicalorder.converter.MedicalOrderConverter;
import com.aimedical.modules.medicalorder.dto.ChargePreOrderDTO;
import com.aimedical.modules.medicalorder.dto.MedicalOrderDTO;
import com.aimedical.modules.medicalorder.dto.MedicalOrderItemDTO;
import com.aimedical.modules.medicalorder.dto.MedicationOrderDTO;
import com.aimedical.modules.medicalorder.entity.ChargePreOrder;
import com.aimedical.modules.medicalorder.entity.ChargePreOrderItem;
import com.aimedical.modules.medicalorder.entity.ChargeItemType;
import com.aimedical.modules.medicalorder.entity.ChargeStatus;
import com.aimedical.modules.medicalorder.entity.MedicalOrder;
import com.aimedical.modules.medicalorder.entity.MedicalOrderItem;
import com.aimedical.modules.medicalorder.entity.OrderStatus;
import com.aimedical.modules.medicalorder.entity.OrderType;
import com.aimedical.modules.medicalorder.repository.ChargePreOrderItemRepository;
import com.aimedical.modules.medicalorder.repository.ChargePreOrderRepository;
import com.aimedical.modules.medicalorder.repository.MedicalOrderItemRepository;
import com.aimedical.modules.medicalorder.repository.MedicalOrderRepository;
import com.aimedical.modules.medicalorder.service.MedicalOrderService;
import com.aimedical.modules.patient.repository.PatientRepository;
import com.aimedical.modules.registration.entity.Registration;
import com.aimedical.modules.registration.entity.RegistrationStatus;
import com.aimedical.modules.registration.repository.RegistrationRepository;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MedicalOrderServiceImpl implements MedicalOrderService {

    private final MedicalOrderRepository orderRepository;
    private final MedicalOrderItemRepository itemRepository;
    private final ChargePreOrderRepository chargePreOrderRepository;
    private final ChargePreOrderItemRepository chargePreOrderItemRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final RegistrationRepository registrationRepository;

    public MedicalOrderServiceImpl(MedicalOrderRepository orderRepository,
                                   MedicalOrderItemRepository itemRepository,
                                   ChargePreOrderRepository chargePreOrderRepository,
                                   ChargePreOrderItemRepository chargePreOrderItemRepository,
                                   PatientRepository patientRepository,
                                   DoctorRepository doctorRepository,
                                   RegistrationRepository registrationRepository) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.chargePreOrderRepository = chargePreOrderRepository;
        this.chargePreOrderItemRepository = chargePreOrderItemRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.registrationRepository = registrationRepository;
    }

    @Override
    @Transactional
    public MedicalOrderDTO createOrder(MedicalOrderDTO dto) {
        if (dto.getPatientId() == null) {
            throw new BusinessException(GlobalErrorCode.PARAM_INVALID, "患者ID不能为空");
        }
        if (dto.getDoctorId() == null) {
            throw new BusinessException(GlobalErrorCode.PARAM_INVALID, "医生ID不能为空");
        }
        if (dto.getRegistrationId() == null) {
            throw new BusinessException(GlobalErrorCode.PARAM_INVALID, "挂号记录ID不能为空");
        }

        // 校验关联实体是否存在
        patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "患者不存在: " + dto.getPatientId()));
        doctorRepository.findById(dto.getDoctorId())
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "医生不存在: " + dto.getDoctorId()));
        Registration registration = registrationRepository.findById(dto.getRegistrationId())
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "挂号记录不存在: " + dto.getRegistrationId()));
        if (!registration.getPatientId().equals(dto.getPatientId())) {
            throw new BusinessException(GlobalErrorCode.PARAM_INVALID, "挂号记录与患者不匹配");
        }
        // 校验挂号状态是否允许创建医嘱
        RegistrationStatus regStatus = registration.getStatus();
        if (regStatus == RegistrationStatus.CANCELLED
                || regStatus == RegistrationStatus.NO_SHOW) {
            throw new BusinessException(GlobalErrorCode.REGISTRATION_STATUS_INVALID,
                    "当前挂号状态不允许创建医嘱: " + regStatus.getDesc());
        }

        MedicalOrder order = MedicalOrderConverter.toEntity(dto);
        order.setOrderStatus(OrderStatus.DRAFT);
        order.setOrderNo(generateOrderNo("MO"));

        BigDecimal totalAmount = BigDecimal.ZERO;
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (MedicalOrderItemDTO itemDto : dto.getItems()) {
                BigDecimal amount = itemDto.getUnitPrice() != null && itemDto.getQuantity() != null
                        ? itemDto.getUnitPrice().multiply(itemDto.getQuantity())
                        : BigDecimal.ZERO;
                totalAmount = totalAmount.add(amount);
            }
        }
        order.setTotalAmount(totalAmount);
        order = orderRepository.save(order);

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            for (MedicalOrderItemDTO itemDto : dto.getItems()) {
                MedicalOrderItem item = MedicalOrderConverter.toEntity(itemDto);
                item.setOrderId(order.getId());
                BigDecimal amount = item.getUnitPrice() != null && item.getQuantity() != null
                        ? item.getUnitPrice().multiply(item.getQuantity())
                        : BigDecimal.ZERO;
                item.setAmount(amount);
                itemRepository.save(item);
            }
        }

        return enrichOrderDto(order);
    }

    @Override
    public MedicalOrderDTO getOrder(Long id) {
        MedicalOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "订单不存在: " + id));
        return enrichOrderDto(order);
    }

    @Override
    @Transactional
    public MedicalOrderDTO submitOrder(Long id) {
        MedicalOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "订单不存在: " + id));

        if (order.getOrderStatus() != OrderStatus.DRAFT) {
            throw new BusinessException(GlobalErrorCode.ORDER_STATUS_INVALID, "只有草稿状态的订单才能提交");
        }

        List<MedicalOrderItem> items = itemRepository.findByOrderId(id);
        if (items == null || items.isEmpty()) {
            throw new BusinessException(GlobalErrorCode.ORDER_ITEM_EMPTY, "订单至少需要包含一个项目");
        }

        BigDecimal totalAmount = items.stream()
                .map(item -> item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setOrderStatus(OrderStatus.SUBMITTED);
        order.setTotalAmount(totalAmount);
        order = saveOrderWithOptimisticLock(order, "提交订单");

        return enrichOrderDto(order);
    }

    @Override
    @Transactional
    public MedicalOrderDTO cancelOrder(Long id) {
        MedicalOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "订单不存在: " + id));

        OrderStatus status = order.getOrderStatus();
        if (status != OrderStatus.DRAFT && status != OrderStatus.SUBMITTED) {
            throw new BusinessException(GlobalErrorCode.ORDER_STATUS_INVALID, "只有草稿或已提交状态的订单才能取消");
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        order = saveOrderWithOptimisticLock(order, "取消订单");

        return enrichOrderDto(order);
    }

    @Override
    @Transactional
    public MedicalOrderDTO chargeOrder(Long id) {
        MedicalOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "订单不存在: " + id));

        if (order.getOrderStatus() != OrderStatus.SUBMITTED) {
            throw new BusinessException(GlobalErrorCode.ORDER_STATUS_INVALID, "只有已提交的订单才能收费");
        }

        order.setOrderStatus(OrderStatus.CHARGED);
        order = saveOrderWithOptimisticLock(order, "收费");
        return enrichOrderDto(order);
    }

    @Override
    @Transactional
    public MedicalOrderDTO dispenseOrder(Long id) {
        MedicalOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "订单不存在: " + id));

        if (order.getOrderStatus() != OrderStatus.CHARGED) {
            throw new BusinessException(GlobalErrorCode.ORDER_STATUS_INVALID, "只有已收费的订单才能发药");
        }

        order.setOrderStatus(OrderStatus.DISPENSED);
        order = saveOrderWithOptimisticLock(order, "发药");
        return enrichOrderDto(order);
    }

    @Override
    @Transactional
    public MedicalOrderDTO completeOrder(Long id) {
        MedicalOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "订单不存在: " + id));

        OrderStatus status = order.getOrderStatus();
        if (status != OrderStatus.CHARGED && status != OrderStatus.DISPENSED) {
            throw new BusinessException(GlobalErrorCode.ORDER_STATUS_INVALID, "只有已收费或已发药的订单才能完成");
        }

        order.setOrderStatus(OrderStatus.COMPLETED);
        order = saveOrderWithOptimisticLock(order, "完成订单");
        return enrichOrderDto(order);
    }

    @Override
    @Transactional
    public ChargePreOrderDTO generateChargePreOrder(Long orderId) {
        MedicalOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "订单不存在: " + orderId));

        if (order.getOrderStatus() != OrderStatus.SUBMITTED) {
            throw new BusinessException(GlobalErrorCode.ORDER_STATUS_INVALID, "只有已提交的订单才能生成收费前置单");
        }

        List<MedicalOrderItem> items = itemRepository.findByOrderId(orderId);
        if (items == null || items.isEmpty()) {
            throw new BusinessException(GlobalErrorCode.ORDER_ITEM_EMPTY, "订单没有项目，无法生成收费预订单");
        }

        ChargePreOrder chargePreOrder = new ChargePreOrder();
        chargePreOrder.setOrderId(orderId);
        chargePreOrder.setPatientId(order.getPatientId());
        chargePreOrder.setChargeNo(generateOrderNo("CP"));
        chargePreOrder.setChargeStatus(ChargeStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<ChargePreOrderItem> chargeItems = new ArrayList<>();
        for (MedicalOrderItem item : items) {
            ChargePreOrderItem chargeItem = new ChargePreOrderItem();
            chargeItem.setChargePreOrderId(null);
            chargeItem.setOrderItemId(item.getId());
            chargeItem.setItemName(item.getItemName());
            chargeItem.setQuantity(item.getQuantity());
            chargeItem.setUnitPrice(item.getUnitPrice());
            chargeItem.setAmount(item.getAmount());
            chargeItem.setChargeItemType(convertToChargeItemType(item.getItemType()));
            chargeItems.add(chargeItem);

            totalAmount = totalAmount.add(item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
        }

        chargePreOrder.setTotalAmount(totalAmount);

        // 幂等性保护：利用数据库唯一约束防止并发重复创建
        try {
            chargePreOrder = chargePreOrderRepository.save(chargePreOrder);
        } catch (DataIntegrityViolationException e) {
            // 唯一约束冲突，说明已有其他并发请求创建了前置单
            return chargePreOrderRepository.findByOrderId(orderId)
                    .map(existing -> {
                        ChargePreOrderDTO dto = MedicalOrderConverter.toDto(existing);
                        List<ChargePreOrderItem> savedItems = chargePreOrderItemRepository.findByChargePreOrderId(existing.getId());
                        dto.setItems(MedicalOrderConverter.toChargeItemDtoList(savedItems));
                        return dto;
                    })
                    .orElseThrow(() -> new BusinessException(GlobalErrorCode.SYSTEM_ERROR, "收费前置单创建失败"));
        }

        for (ChargePreOrderItem chargeItem : chargeItems) {
            chargeItem.setChargePreOrderId(chargePreOrder.getId());
            chargePreOrderItemRepository.save(chargeItem);
        }

        ChargePreOrderDTO dto = MedicalOrderConverter.toDto(chargePreOrder);
        List<ChargePreOrderItem> savedItems = chargePreOrderItemRepository.findByChargePreOrderId(chargePreOrder.getId());
        dto.setItems(MedicalOrderConverter.toChargeItemDtoList(savedItems));
        return dto;
    }

    @Override
    public Page<MedicalOrderDTO> getOrdersByPatient(Long patientId, Pageable pageable) {
        return orderRepository.findByPatientId(patientId, pageable).map(order -> {
            MedicalOrderDTO dto = MedicalOrderConverter.toDto(order);
            List<MedicalOrderItem> items = itemRepository.findByOrderId(order.getId());
            dto.setItems(MedicalOrderConverter.toItemDtoList(items));
            return dto;
        });
    }

    @Override
    public Page<MedicalOrderDTO> getOrdersByDoctor(Long doctorId, Pageable pageable) {
        return orderRepository.findByDoctorId(doctorId, pageable).map(order -> {
            MedicalOrderDTO dto = MedicalOrderConverter.toDto(order);
            List<MedicalOrderItem> items = itemRepository.findByOrderId(order.getId());
            dto.setItems(MedicalOrderConverter.toItemDtoList(items));
            return dto;
        });
    }

    @Override
    public MedicationOrderDTO buildMedicationOrderContract(Long orderId) {
        MedicalOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "订单不存在: " + orderId));

        List<MedicalOrderItem> items = itemRepository.findByOrderId(orderId);

        String patientName = null;
        if (order.getPatientId() != null) {
            patientName = patientRepository.findById(order.getPatientId())
                    .map(patient -> patient.getRealName())
                    .orElse(null);
        }
        String doctorName = null;
        if (order.getDoctorId() != null) {
            doctorName = doctorRepository.findById(order.getDoctorId())
                    .map(doctor -> doctor.getRealName())
                    .orElse(null);
        }

        return MedicalOrderConverter.toMedicationOrderDTO(order, items, patientName, doctorName);
    }

    private MedicalOrderDTO enrichOrderDto(MedicalOrder order) {
        MedicalOrderDTO dto = MedicalOrderConverter.toDto(order);
        List<MedicalOrderItem> items = itemRepository.findByOrderId(order.getId());
        dto.setItems(MedicalOrderConverter.toItemDtoList(items));
        return dto;
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private String generateOrderNo(String prefix) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String timestamp = String.format("%06d", LocalDateTime.now().getNano() / 1000 % 1000000);
        String random = String.format("%06d", SECURE_RANDOM.nextInt(1000000));
        return prefix + "-" + date + "-" + timestamp + "-" + random;
    }

    /**
     * 带乐观锁保护的订单保存操作，并发冲突时抛出 BusinessException
     */
    private MedicalOrder saveOrderWithOptimisticLock(MedicalOrder order, String operation) {
        try {
            return orderRepository.save(order);
        } catch (OptimisticLockException e) {
            throw new BusinessException(GlobalErrorCode.SYSTEM_ERROR,
                    operation + "失败：数据已被其他操作修改，请刷新后重试");
        }
    }

    private ChargeItemType convertToChargeItemType(OrderType orderType) {
        if (orderType == null) {
            return ChargeItemType.OTHER;
        }
        return switch (orderType) {
            case DRUG -> ChargeItemType.DRUG;
            case EXAMINATION -> ChargeItemType.EXAMINATION;
            case LAB_TEST -> ChargeItemType.LAB_TEST;
        };
    }

}