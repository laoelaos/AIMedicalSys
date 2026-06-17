package com.aimedical.modules.doctor.repository;

import com.aimedical.modules.doctor.entity.DoctorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<DoctorEntity, Long> {
}
