package com.aimedical.modules.prescription.repository;

import com.aimedical.modules.prescription.rule.entity.DrugAllergyMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DrugAllergyMappingRepository extends JpaRepository<DrugAllergyMapping, Long> {

    Optional<DrugAllergyMapping> findByDrugCode(String drugCode);
}
