package com.aimedical.modules.prescription.repository;

import com.aimedical.common.entity.DosageStandard;
import org.springframework.data.repository.Repository;
import java.util.List;

public interface DosageStandardRepository extends Repository<DosageStandard, Long> {

    List<DosageStandard> findByDrugCodeAndRouteOfAdministration(String drugCode, String routeOfAdministration);

    List<DosageStandard> findByDrugCodeAndRouteOfAdministrationAndAgeRangeStartNotNullAndAgeRangeEndNotNull(
        String drugCode, String routeOfAdministration);
}
