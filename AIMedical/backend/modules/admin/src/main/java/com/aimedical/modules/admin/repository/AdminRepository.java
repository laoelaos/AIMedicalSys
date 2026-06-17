package com.aimedical.modules.admin.repository;

import com.aimedical.modules.admin.entity.AdminEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<AdminEntity, Long> {
}
