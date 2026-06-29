package com.aimedical.modules.patient.repository;

import com.aimedical.modules.patient.entity.RegistrationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistrationRepository extends JpaRepository<RegistrationEntity, Long> {

    List<RegistrationEntity> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(Long userId);
}
