package com.aimedical.modules.prescription.repository;

import com.aimedical.modules.prescription.rule.entity.DrugContraindicationMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DrugContraindicationMappingRepository extends JpaRepository<DrugContraindicationMapping, Long> {

    Optional<DrugContraindicationMapping> findByDrugCode(String drugCode);
}
