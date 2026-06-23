package com.aimedical.modules.commonmodule.permission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 岗位数据访问层
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * 根据岗位代码查询岗位
     *
     * @param code 岗位代码
     * @return 岗位信息
     */
    Optional<Post> findByCode(String code);

    /**
     * 根据角色ID查询岗位列表
     *
     * @param roleId 角色ID
     * @return 岗位列表
     */
    List<Post> findByRoleId(Long roleId);

    /**
     * 检查岗位代码是否存在
     *
     * @param code 岗位代码
     * @return 是否存在
     */
    boolean existsByCode(String code);
}