package com.aimedical.modules.admin.entity.dict;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DictTypeRepository extends JpaRepository<DictType, Long> {
}
