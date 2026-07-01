package com.aimedical.modules.prescription.cache;

import com.aimedical.modules.prescription.repository.DrugAllergyMappingRepository;
import com.aimedical.modules.prescription.repository.DrugCompositionDictRepository;
import com.aimedical.modules.prescription.repository.DrugContraindicationMappingRepository;
import com.aimedical.modules.prescription.rule.entity.DrugAllergyMapping;
import com.aimedical.modules.prescription.rule.entity.DrugCompositionDict;
import com.aimedical.modules.prescription.rule.entity.DrugContraindicationMapping;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class DrugDictCacheManager {

    private final LoadingCache<String, DrugContraindicationMapping> contraindicationCache;
    private final LoadingCache<String, DrugAllergyMapping> allergyMappingCache;
    private final LoadingCache<String, DrugCompositionDict> compositionDictCache;

    public DrugDictCacheManager(
            DrugContraindicationMappingRepository contraindicationRepo,
            DrugAllergyMappingRepository allergyMappingRepo,
            DrugCompositionDictRepository compositionDictRepo) {
        this.contraindicationCache = Caffeine.newBuilder()
                .refreshAfterWrite(60, TimeUnit.MINUTES)
                .build(key -> contraindicationRepo.findByDrugCode(key).orElse(null));
        this.allergyMappingCache = Caffeine.newBuilder()
                .refreshAfterWrite(60, TimeUnit.MINUTES)
                .build(key -> allergyMappingRepo.findByDrugCode(key).orElse(null));
        this.compositionDictCache = Caffeine.newBuilder()
                .refreshAfterWrite(60, TimeUnit.MINUTES)
                .build(key -> compositionDictRepo.findByDrugCode(key).orElse(null));
    }

    public DrugContraindicationMapping getContraindication(String drugCode) {
        return contraindicationCache.get(drugCode);
    }

    public DrugAllergyMapping getAllergyMapping(String drugCode) {
        return allergyMappingCache.get(drugCode);
    }

    public DrugCompositionDict getCompositionDict(String drugCode) {
        return compositionDictCache.get(drugCode);
    }

    public void invalidateAll() {
        contraindicationCache.invalidateAll();
        allergyMappingCache.invalidateAll();
        compositionDictCache.invalidateAll();
    }

    public void invalidateContraindication(String drugCode) {
        contraindicationCache.invalidate(drugCode);
    }

    public void invalidateAllergyMapping(String drugCode) {
        allergyMappingCache.invalidate(drugCode);
    }

    public void invalidateCompositionDict(String drugCode) {
        compositionDictCache.invalidate(drugCode);
    }
}
