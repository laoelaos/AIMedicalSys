package com.aimedical.modules.prescription.repository;

import com.aimedical.modules.prescription.rule.entity.DrugCompositionDict;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DrugCompositionDictRepository extends JpaRepository<DrugCompositionDict, Long> {

    Optional<DrugCompositionDict> findByDrugCode(String drugCode);
}
