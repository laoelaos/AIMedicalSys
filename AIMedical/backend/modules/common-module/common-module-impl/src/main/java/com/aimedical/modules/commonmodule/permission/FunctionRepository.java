package com.aimedical.modules.commonmodule.permission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 功能权限数据访问层
 */
@Repository
public interface FunctionRepository extends JpaRepository<Function, Long> {

    /**
     * 根据功能代码查询功能
     *
     * @param code 功能代码
     * @return 功能信息
     */
    Optional<Function> findByCode(String code);

    /**
     * 根据父功能ID查询子功能列表
     *
     * @param parentId 父功能ID
     * @return 子功能列表
     */
    List<Function> findByParentId(Long parentId);

    /**
     * 根据可见性查询功能列表
     *
     * @param visible 是否可见
     * @return 功能列表
     */
    List<Function> findByVisible(Boolean visible);

    /**
     * 检查功能代码是否存在
     *
     * @param code 功能代码
     * @return 是否存在
     */
    boolean existsByCode(String code);
}