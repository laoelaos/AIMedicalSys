package com.aimedical.modules.medicalrecord.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aimedical.modules.medicalrecord.entity.DeptTemplateConfig;

@Repository
public interface DeptTemplateConfigRepository extends JpaRepository<DeptTemplateConfig, Long> {
    Optional<DeptTemplateConfig> findByDepartmentId(String departmentId);
}
