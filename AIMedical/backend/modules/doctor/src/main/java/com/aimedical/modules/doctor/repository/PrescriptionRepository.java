package com.aimedical.modules.doctor.repository;

import com.aimedical.modules.doctor.entity.PrescriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 处方仓储
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public interface PrescriptionRepository extends JpaRepository<PrescriptionEntity, Long> {

    /**
     * 按患者ID查询处方列表（按创建时间倒序）
     *
     * @param patientId 患者档案ID
     * @return 处方列表
     */
    List<PrescriptionEntity> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    /**
     * 按医生ID查询处方列表
     *
     * @param doctorId 医生用户ID
     * @return 处方列表
     */
    List<PrescriptionEntity> findByDoctorIdOrderByCreatedAtDesc(Long doctorId);
}
