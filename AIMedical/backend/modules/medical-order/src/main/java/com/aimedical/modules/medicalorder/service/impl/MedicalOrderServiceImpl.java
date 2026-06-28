package com.aimedical.modules.medicalorder.service.impl;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.modules.doctor.entity.DoctorEntity;
import com.aimedical.modules.doctor.repository.DoctorRepository;
import com.aimedical.modules.medicalorder.converter.MedicalOrderConverter;
import com.aimedical.modules.medicalorder.dto.ChargePreOrderDTO;
import com.aimedical.modules.medicalorder.dto.MedicalOrderDTO;
import com.aimedical.modules.medicalorder.dto.MedicalOrderItemDTO;
import com.aimedical.modules.medicalorder.dto.MedicationOrderDTO;
import com.aimedical.modules.medicalorder.entity.ChargePreOrder;
import com.aimedical.modules.medicalorder.entity.ChargePreOrderItem;
import com.aimedical.modules.medicalorder.entity.MedicalOrder;
import com.aimedical.modules.medicalorder.entity.MedicalOrderItem;
import com.aimedical.modules.medicalorder.repository.ChargePreOrderItemRepository;
import com.aimedical.modules.medicalorder.repository.ChargePreOrderRepository;
import com.aimedical.modules.medicalorder.repository.MedicalOrderItemRepository;
import com.aimedical.modules.medicalorder.repository.MedicalOrderRepository;
import com.aimedical.modules.medicalorder.service.MedicalOrderService;
import com.aimedical.modules.patient.entity.PatientEntity;
import com.aimedical.modules.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
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

    public MedicalOrderServiceImpl(MedicalOrderRepository orderRepository,
                                   MedicalOrderItemRepository itemRepository,
                                   ChargePreOrderRepository chargePreOrderRepository,
                                   ChargePreOrderItemRepository chargePreOrderItemRepository,
                                   PatientRepository patientRepository,
                                   DoctorRepository doctorRepository) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.chargePreOrderRepository = chargePreOrderRepository;
        this.chargePreOrderItemRepository = chargePreOrderItemRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
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

        MedicalOrder order = MedicalOrderConverter.toEntity(dto);
        order.setOrderStatus("DRAFT");
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

        if (!"DRAFT".equals(order.getOrderStatus())) {
            throw new BusinessException(GlobalErrorCode.ORDER_STATUS_INVALID, "只有草稿状态的订单才能提交");
        }

        List<MedicalOrderItem> items = itemRepository.findByOrderId(id);
        if (items == null || items.isEmpty()) {
            throw new BusinessException(GlobalErrorCode.ORDER_ITEM_EMPTY, "订单至少需要包含一个项目");
        }

        BigDecimal totalAmount = items.stream()
                .map(item -> item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setOrderStatus("SUBMITTED");
        order.setTotalAmount(totalAmount);
        order = orderRepository.save(order);

        return enrichOrderDto(order);
    }

    @Override
    @Transactional
    public MedicalOrderDTO cancelOrder(Long id) {
        MedicalOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "订单不存在: " + id));

        String status = order.getOrderStatus();
        if (!"DRAFT".equals(status) && !"SUBMITTED".equals(status)) {
            throw new BusinessException(GlobalErrorCode.ORDER_STATUS_INVALID, "只有草稿或已提交状态的订单才能取消");
        }

        order.setOrderStatus("CANCELLED");
        order = orderRepository.save(order);

        return enrichOrderDto(order);
    }

    @Override
    @Transactional
    public ChargePreOrderDTO generateChargePreOrder(Long orderId) {
        MedicalOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "订单不存在: " + orderId));

        // 幂等性检查：如果已生成过收费前置单，直接返回已有记录
        chargePreOrderRepository.findByOrderId(orderId).ifPresent(existing -> {
            throw new BusinessException(GlobalErrorCode.CHARGE_PRE_ORDER_EXISTS,
                    "该订单已生成收费前置单: " + existing.getChargeNo());
        });

        List<MedicalOrderItem> items = itemRepository.findByOrderId(orderId);
        if (items == null || items.isEmpty()) {
            throw new BusinessException(GlobalErrorCode.ORDER_ITEM_EMPTY, "订单没有项目，无法生成收费预订单");
        }

        ChargePreOrder chargePreOrder = new ChargePreOrder();
        chargePreOrder.setOrderId(orderId);
        chargePreOrder.setPatientId(order.getPatientId());
        chargePreOrder.setChargeNo(generateOrderNo("CP"));
        chargePreOrder.setChargeStatus("PENDING");

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
            chargeItem.setChargeItemType(item.getItemType());
            chargeItems.add(chargeItem);

            totalAmount = totalAmount.add(item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
        }

        chargePreOrder.setTotalAmount(totalAmount);
        chargePreOrder = chargePreOrderRepository.save(chargePreOrder);

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
    public List<MedicalOrderDTO> getOrdersByPatient(Long patientId) {
        List<MedicalOrder> orders = orderRepository.findByPatientId(patientId);
        return orders.stream().map(order -> {
            MedicalOrderDTO dto = MedicalOrderConverter.toDto(order);
            List<MedicalOrderItem> items = itemRepository.findByOrderId(order.getId());
            dto.setItems(MedicalOrderConverter.toItemDtoList(items));
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<MedicalOrderDTO> getOrdersByDoctor(Long doctorId) {
        List<MedicalOrder> orders = orderRepository.findByDoctorId(doctorId);
        return orders.stream().map(order -> {
            MedicalOrderDTO dto = MedicalOrderConverter.toDto(order);
            List<MedicalOrderItem> items = itemRepository.findByOrderId(order.getId());
            dto.setItems(MedicalOrderConverter.toItemDtoList(items));
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public MedicationOrderDTO buildMedicationOrderContract(Long orderId) {
        MedicalOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "订单不存在: " + orderId));

        List<MedicalOrderItem> items = itemRepository.findByOrderId(orderId);

        MedicationOrderDTO contract = new MedicationOrderDTO();
        contract.setOrderNo(order.getOrderNo());
        contract.setPatientId(order.getPatientId());
        contract.setDoctorId(order.getDoctorId());
        contract.setDiagnosis(order.getDiagnosis());
        contract.setIsUrgent(order.getIsUrgent());

        // 填充患者姓名
        if (order.getPatientId() != null) {
            patientRepository.findById(order.getPatientId())
                    .ifPresent(patient -> contract.setPatientName(patient.getRealName()));
        }
        // 填充医生姓名
        if (order.getDoctorId() != null) {
            doctorRepository.findById(order.getDoctorId())
                    .ifPresent(doctor -> contract.setDoctorName(doctor.getRealName()));
        }

        if (items != null) {
            List<MedicationOrderDTO.MedicationOrderItemDTO> contractItems = items.stream().map(item -> {
                MedicationOrderDTO.MedicationOrderItemDTO contractItem = new MedicationOrderDTO.MedicationOrderItemDTO();
                contractItem.setItemCode(item.getItemCode());
                contractItem.setItemName(item.getItemName());
                contractItem.setSpecification(item.getSpecification());
                contractItem.setQuantity(item.getQuantity());
                contractItem.setUnit(item.getUnit());
                contractItem.setDosage(item.getDosage());
                contractItem.setUsageMethod(item.getUsageMethod());
                contractItem.setFrequency(item.getFrequency());
                contractItem.setDays(item.getDays());
                return contractItem;
            }).collect(Collectors.toList());
            contract.setItems(contractItems);
        }

        return contract;
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
        String random = String.format("%05d", SECURE_RANDOM.nextInt(100000));
        return prefix + "-" + date + "-" + random;
    }

}