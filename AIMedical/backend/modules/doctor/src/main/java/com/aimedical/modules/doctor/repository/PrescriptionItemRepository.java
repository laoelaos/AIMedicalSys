package com.aimedical.modules.doctor.repository;

import com.aimedical.modules.doctor.entity.PrescriptionItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 处方明细仓储
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItemEntity, Long> {

    /**
     * 按处方ID查询明细列表
     *
     * @param prescriptionId 处方ID
     * @return 明细列表
     */
    List<PrescriptionItemEntity> findByPrescriptionId(Long prescriptionId);

    /**
     * 按处方ID删除全部明细
     *
     * @param prescriptionId 处方ID
     */
    void deleteByPrescriptionId(Long prescriptionId);
}
