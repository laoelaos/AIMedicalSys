package com.aimedical.modules.commonmodule.permission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionFunctionRepository extends JpaRepository<PermissionFunction, Long> {

    Optional<PermissionFunction> findByCode(String code);

    List<PermissionFunction> findByParentId(Long parentId);

    List<PermissionFunction> findByVisible(Boolean visible);

    boolean existsByCode(String code);
}
