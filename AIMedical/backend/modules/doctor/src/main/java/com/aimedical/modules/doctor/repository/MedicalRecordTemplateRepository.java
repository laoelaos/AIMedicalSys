package com.aimedical.modules.doctor.repository;

import com.aimedical.modules.doctor.entity.MedicalRecordTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 病历模板仓储
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public interface MedicalRecordTemplateRepository extends JpaRepository<MedicalRecordTemplateEntity, Long> {

    /**
     * 按科室查询启用的模板列表
     *
     * @param department 科室
     * @param enabled    是否启用
     * @return 模板列表
     */
    List<MedicalRecordTemplateEntity> findByDepartmentAndEnabled(String department, Boolean enabled);

    /**
     * 查询所有启用的模板列表（department 为空时使用）
     *
     * @param enabled 是否启用
     * @return 模板列表
     */
    List<MedicalRecordTemplateEntity> findByEnabled(Boolean enabled);
}
