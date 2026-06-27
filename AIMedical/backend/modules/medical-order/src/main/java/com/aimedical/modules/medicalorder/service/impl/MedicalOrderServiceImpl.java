package com.aimedical.modules.medicalorder.service.impl;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.modules.medicalorder.converter.MedicalOrderConverter;
import com.aimedical.modules.medicalorder.dto.ChargePreOrderDTO;
import com.aimedical.modules.medicalorder.dto.ChargePreOrderItemDTO;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class MedicalOrderServiceImpl implements MedicalOrderService {

    private final MedicalOrderRepository orderRepository;
    private final MedicalOrderItemRepository itemRepository;
    private final ChargePreOrderRepository chargePreOrderRepository;
    private final ChargePreOrderItemRepository chargePreOrderItemRepository;

    public MedicalOrderServiceImpl(MedicalOrderRepository orderRepository,
                                   MedicalOrderItemRepository itemRepository,
                                   ChargePreOrderRepository chargePreOrderRepository,
                                   ChargePreOrderItemRepository chargePreOrderItemRepository) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.chargePreOrderRepository = chargePreOrderRepository;
        this.chargePreOrderItemRepository = chargePreOrderItemRepository;
    }

    @Override
    @Transactional
    public MedicalOrderDTO createOrder(MedicalOrderDTO dto) {
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
            throw new BusinessException(GlobalErrorCode.SYSTEM_ERROR, "只有草稿状态的订单才能提交");
        }

        List<MedicalOrderItem> items = itemRepository.findByOrderId(id);
        if (items == null || items.isEmpty()) {
            throw new BusinessException(GlobalErrorCode.SYSTEM_ERROR, "订单至少需要包含一个项目");
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
            throw new BusinessException(GlobalErrorCode.SYSTEM_ERROR, "只有草稿或已提交状态的订单才能取消");
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

        List<MedicalOrderItem> items = itemRepository.findByOrderId(orderId);
        if (items == null || items.isEmpty()) {
            throw new BusinessException(GlobalErrorCode.SYSTEM_ERROR, "订单没有项目，无法生成收费预订单");
        }

        ChargePreOrder chargePreOrder = new ChargePreOrder();
        chargePreOrder.setOrderId(orderId);
        chargePreOrder.setPatientId(order.getPatientId());
        chargePreOrder.setChargeNo(generateOrderNo("CP"));
        chargePreOrder.setChargeStatus("CHARGED");

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

    private String generateOrderNo(String prefix) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%05d", new Random().nextInt(100000));
        return prefix + "-" + date + "-" + random;
    }

}