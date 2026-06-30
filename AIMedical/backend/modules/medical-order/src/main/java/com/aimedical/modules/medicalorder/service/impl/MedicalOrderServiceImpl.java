package com.aimedical.modules.medicalorder.service.impl;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.modules.doctor.service.DoctorService;
import com.aimedical.modules.medicalorder.converter.MedicalOrderConverter;
import com.aimedical.modules.medicalorder.dto.ChargePreOrderDTO;
import com.aimedical.modules.medicalorder.dto.MedicalOrderCreateRequest;
import com.aimedical.modules.medicalorder.dto.MedicalOrderDTO;
import com.aimedical.modules.medicalorder.dto.MedicalOrderItemDTO;
import com.aimedical.modules.medicalorder.dto.MedicationOrderDTO;
import com.aimedical.modules.medicalorder.entity.ChargePreOrder;
import com.aimedical.modules.medicalorder.entity.ChargePreOrderItem;
import com.aimedical.modules.medicalorder.entity.ChargeItemType;
import com.aimedical.modules.medicalorder.entity.ChargeStatus;
import com.aimedical.modules.medicalorder.entity.ItemType;
import com.aimedical.modules.medicalorder.entity.MedicalOrder;
import com.aimedical.modules.medicalorder.entity.MedicalOrderItem;
import com.aimedical.modules.medicalorder.entity.OrderStatus;
import com.aimedical.modules.medicalorder.repository.ChargePreOrderItemRepository;
import com.aimedical.modules.medicalorder.repository.ChargePreOrderRepository;
import com.aimedical.modules.medicalorder.repository.MedicalOrderItemRepository;
import com.aimedical.modules.medicalorder.repository.MedicalOrderRepository;
import com.aimedical.modules.medicalorder.service.MedicalOrderService;
import com.aimedical.modules.patient.service.PatientService;
import com.aimedical.modules.registration.entity.Registration;
import com.aimedical.modules.registration.entity.RegistrationStatus;
import com.aimedical.modules.registration.repository.RegistrationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MedicalOrderServiceImpl implements MedicalOrderService {

    private final MedicalOrderRepository orderRepository;
    private final MedicalOrderItemRepository itemRepository;
    private final ChargePreOrderRepository chargePreOrderRepository;
    private final ChargePreOrderItemRepository chargePreOrderItemRepository;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final RegistrationRepository registrationRepository;

    /** 合法的状态转换矩阵：from -> {to1, to2, ...} */
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.DRAFT, Set.of(OrderStatus.SUBMITTED, OrderStatus.CANCELLED),
            OrderStatus.SUBMITTED, Set.of(OrderStatus.CHARGED, OrderStatus.CANCELLED),
            OrderStatus.CHARGED, Set.of(OrderStatus.DISPENSED, OrderStatus.COMPLETED),
            OrderStatus.DISPENSED, Set.of(OrderStatus.COMPLETED)
    );

    public MedicalOrderServiceImpl(MedicalOrderRepository orderRepository,
                                   MedicalOrderItemRepository itemRepository,
                                   ChargePreOrderRepository chargePreOrderRepository,
                                   ChargePreOrderItemRepository chargePreOrderItemRepository,
                                   PatientService patientService,
                                   DoctorService doctorService,
                                   RegistrationRepository registrationRepository) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.chargePreOrderRepository = chargePreOrderRepository;
        this.chargePreOrderItemRepository = chargePreOrderItemRepository;
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.registrationRepository = registrationRepository;
    }

    @Override
    @Transactional
    public MedicalOrderDTO createOrder(MedicalOrderCreateRequest dto) {
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
        if (!patientService.existsById(dto.getPatientId())) {
            throw new BusinessException(GlobalErrorCode.NOT_FOUND, "患者不存在: " + dto.getPatientId());
        }
        if (!doctorService.existsById(dto.getDoctorId())) {
            throw new BusinessException(GlobalErrorCode.NOT_FOUND, "医生不存在: " + dto.getDoctorId());
        }
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
        order = saveOrderWithRetry(order);

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            List<MedicalOrderItem> itemsToSave = new ArrayList<>();
            for (MedicalOrderItemDTO itemDto : dto.getItems()) {
                MedicalOrderItem item = MedicalOrderConverter.toEntity(itemDto);
                item.setOrderId(order.getId());
                BigDecimal amount = item.getUnitPrice() != null && item.getQuantity() != null
                        ? item.getUnitPrice().multiply(item.getQuantity())
                        : BigDecimal.ZERO;
                item.setAmount(amount);
                itemsToSave.add(item);
            }
            itemRepository.saveAll(itemsToSave);
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
        validateTransition(order.getOrderStatus(), OrderStatus.SUBMITTED, "提交订单");

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

        // 同步取消关联的收费前置单（仅 PENDING 状态才取消）
        chargePreOrderRepository.findByOrderId(id).ifPresent(chargePreOrder -> {
            if (chargePreOrder.getChargeStatus() == ChargeStatus.PENDING) {
                chargePreOrder.setChargeStatus(ChargeStatus.CANCELLED);
                chargePreOrderRepository.save(chargePreOrder);
            }
        });

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
        validateTransition(order.getOrderStatus(), OrderStatus.CHARGED, "收费");

        order.setOrderStatus(OrderStatus.CHARGED);
        order = saveOrderWithOptimisticLock(order, "收费");

        // 同步更新收费前置单状态
        chargePreOrderRepository.findByOrderId(id).ifPresent(chargePreOrder -> {
            chargePreOrder.setChargeStatus(ChargeStatus.CHARGED);
            chargePreOrderRepository.save(chargePreOrder);
        });

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
        validateTransition(order.getOrderStatus(), OrderStatus.COMPLETED, "完成订单");

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

        // 前置检查：如果已有收费前置单，拒绝重复生成
        if (chargePreOrderRepository.findByOrderId(orderId).isPresent()) {
            throw new BusinessException(GlobalErrorCode.CHARGE_PRE_ORDER_EXISTS);
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

        // 利用数据库唯一约束防止并发重复创建
        try {
            chargePreOrder = chargePreOrderRepository.save(chargePreOrder);
        } catch (DataIntegrityViolationException e) {
            // 并发冲突：其他请求已创建前置单
            throw new BusinessException(GlobalErrorCode.CHARGE_PRE_ORDER_EXISTS);
        }

        for (ChargePreOrderItem chargeItem : chargeItems) {
            chargeItem.setChargePreOrderId(chargePreOrder.getId());
        }
        chargePreOrderItemRepository.saveAll(chargeItems);

        ChargePreOrderDTO dto = MedicalOrderConverter.toDto(chargePreOrder);
        List<ChargePreOrderItem> savedItems = chargePreOrderItemRepository.findByChargePreOrderId(chargePreOrder.getId());
        dto.setItems(MedicalOrderConverter.toChargeItemDtoList(savedItems));
        return dto;
    }

    @Override
    public Page<MedicalOrderDTO> getOrdersByPatient(Long patientId, Pageable pageable) {
        Page<MedicalOrder> orderPage = orderRepository.findByPatientId(patientId, pageable);
        return enrichOrderDtos(orderPage);
    }

    @Override
    public Page<MedicalOrderDTO> getOrdersByDoctor(Long doctorId, Pageable pageable) {
        Page<MedicalOrder> orderPage = orderRepository.findByDoctorId(doctorId, pageable);
        return enrichOrderDtos(orderPage);
    }

    private Page<MedicalOrderDTO> enrichOrderDtos(Page<MedicalOrder> orderPage) {
        if (orderPage.isEmpty()) {
            return orderPage.map(MedicalOrderConverter::toDto);
        }
        List<Long> orderIds = orderPage.getContent().stream()
                .map(MedicalOrder::getId)
                .collect(Collectors.toList());
        List<MedicalOrderItem> allItems = itemRepository.findByOrderIdIn(orderIds);
        return orderPage.map(order -> {
            MedicalOrderDTO dto = MedicalOrderConverter.toDto(order);
            List<MedicalOrderItem> orderItems = allItems.stream()
                    .filter(item -> item.getOrderId().equals(order.getId()))
                    .collect(Collectors.toList());
            dto.setItems(MedicalOrderConverter.toItemDtoList(orderItems));
            return dto;
        });
    }

    @Override
    public MedicationOrderDTO buildMedicationOrderContract(Long orderId) {
        MedicalOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "订单不存在: " + orderId));

        // 处方契约要求订单至少为已提交状态
        OrderStatus status = order.getOrderStatus();
        if (status == OrderStatus.DRAFT || status == OrderStatus.CANCELLED) {
            throw new BusinessException(GlobalErrorCode.ORDER_STATUS_INVALID,
                    "订单状态不允许生成处方契约，需至少为已提交状态");
        }

        List<MedicalOrderItem> items = itemRepository.findByOrderId(orderId).stream()
                .filter(item -> item.getItemType() == ItemType.DRUG)
                .collect(Collectors.toList());

        String patientName = order.getPatientId() != null ? patientService.getRealName(order.getPatientId()) : null;
        String doctorName = order.getDoctorId() != null ? doctorService.getRealName(order.getDoctorId()) : null;

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
     * 校验状态转换是否合法，基于集中定义的 {@link #VALID_TRANSITIONS} 矩阵。
     */
    private void validateTransition(OrderStatus from, OrderStatus to, String operation) {
        Set<OrderStatus> allowed = VALID_TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new BusinessException(GlobalErrorCode.ORDER_STATUS_INVALID,
                    operation + "失败：不允许从 " + from.getDesc() + " 转换到 " + to.getDesc());
        }
    }

    /**
     * 带乐观锁保护的订单保存操作，并发冲突时抛出 BusinessException
     */
    private MedicalOrder saveOrderWithOptimisticLock(MedicalOrder order, String operation) {
        try {
            return orderRepository.save(order);
        } catch (OptimisticLockingFailureException e) {
            throw new BusinessException(GlobalErrorCode.SYSTEM_ERROR,
                    operation + "失败：数据已被其他操作修改，请刷新后重试");
        }
    }

    private MedicalOrder saveOrderWithRetry(MedicalOrder order) {
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                return orderRepository.save(order);
            } catch (DataIntegrityViolationException e) {
                if (i == maxRetries - 1) {
                    throw new BusinessException(GlobalErrorCode.SYSTEM_ERROR, "订单创建失败，请重试");
                }
                order.setOrderNo(generateOrderNo("MO"));
            }
        }
        throw new BusinessException(GlobalErrorCode.SYSTEM_ERROR, "订单创建失败");
    }

    private ChargeItemType convertToChargeItemType(ItemType itemType) {
        if (itemType == null) {
            return ChargeItemType.OTHER;
        }
        return switch (itemType) {
            case DRUG -> ChargeItemType.DRUG;
            case EXAMINATION -> ChargeItemType.EXAMINATION;
            case LAB_TEST -> ChargeItemType.LAB_TEST;
        };
    }

}
