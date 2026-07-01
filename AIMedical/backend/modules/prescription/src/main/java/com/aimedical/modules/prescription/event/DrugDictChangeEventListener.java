package com.aimedical.modules.prescription.event;

import com.aimedical.modules.prescription.cache.DrugDictCacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DrugDictChangeEventListener {

    private final DrugDictCacheManager cacheManager;

    public DrugDictChangeEventListener(DrugDictCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @EventListener
    public void handleContraindicationChange(DrugContraindicationChangeEvent event) {
        cacheManager.invalidateContraindication(event.getDrugCode());
    }

    @EventListener
    public void handleAllergyMappingChange(DrugAllergyMappingChangeEvent event) {
        cacheManager.invalidateAllergyMapping(event.getDrugCode());
    }

    @EventListener
    public void handleCompositionDictChange(DrugCompositionDictChangeEvent event) {
        cacheManager.invalidateCompositionDict(event.getDrugCode());
    }
}
