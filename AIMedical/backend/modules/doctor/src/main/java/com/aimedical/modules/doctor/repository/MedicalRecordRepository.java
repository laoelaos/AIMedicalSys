package com.aimedical.modules.doctor.repository;

import com.aimedical.modules.doctor.entity.MedicalRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 病历仓储
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public interface MedicalRecordRepository extends JpaRepository<MedicalRecordEntity, Long> {

    /**
     * 按患者ID查询病历列表（按版本号倒序，最新版本在前）
     *
     * @param patientId 患者档案ID
     * @return 病历列表
     */
    List<MedicalRecordEntity> findByPatientIdOrderByVersionNoDesc(Long patientId);

    /**
     * 按患者ID和状态查询病历列表
     *
     * @param patientId 患者档案ID
     * @param status    状态 DRAFT/OFFICIAL
     * @return 病历列表
     */
    List<MedicalRecordEntity> findByPatientIdAndStatusOrderByVersionNoDesc(Long patientId, String status);

    /**
     * 按患者ID、医生ID和状态查询病历列表（将 doctorId 下推到数据库，避免内存过滤）
     *
     * @param patientId 患者档案ID
     * @param doctorId  医生用户ID
     * @param status    状态 DRAFT/OFFICIAL
     * @return 病历列表
     */
    List<MedicalRecordEntity> findByPatientIdAndDoctorIdAndStatusOrderByVersionNoDesc(Long patientId,
                                                                                       Long doctorId,
                                                                                       String status);

    /**
     * 按患者ID和医生ID查询病历列表（按版本号倒序）。
     * 用于越权防护：医生仅能查看本人为该患者创建的病历。
     *
     * @param patientId 患者档案ID
     * @param doctorId  医生用户ID
     * @return 病历列表
     */
    List<MedicalRecordEntity> findByPatientIdAndDoctorIdOrderByVersionNoDesc(Long patientId, Long doctorId);

    /**
     * 查询患者最新的正式版本病历（Spring Data 自动生成 LIMIT 1，避免全量载入）。
     * 用于发布时计算下一个版本号。
     *
     * @param patientId 患者档案ID
     * @param status    病历状态
     * @return 最新版本病历（可能为空）
     */
    Optional<MedicalRecordEntity> findFirstByPatientIdAndStatusOrderByVersionNoDesc(Long patientId, String status);
}
