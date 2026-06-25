package com.aimedical.modules.commonmodule.permission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 角色数据访问层
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * 根据角色代码查询角色
     *
     * @param code 角色代码
     * @return 角色信息
     */
    Optional<Role> findByCode(String code);

    /**
     * 检查角色代码是否存在
     *
     * @param code 角色代码
     * @return 是否存在
     */
    boolean existsByCode(String code);
}