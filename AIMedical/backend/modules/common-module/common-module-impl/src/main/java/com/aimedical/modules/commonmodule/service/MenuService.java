package com.aimedical.modules.commonmodule.service;

import com.aimedical.modules.commonmodule.dto.response.MenuResponse;

import java.util.List;

/**
 * 菜单服务接口
 *
 * <p>提供菜单相关的业务操作，包括获取用户菜单树和获取所有菜单。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
public interface MenuService {

    /**
     * 获取用户菜单树
     *
     * @param userId 用户ID
     * @return 菜单树列表
     */
    List<MenuResponse> getUserMenuTree(Long userId);

    /**
     * 获取所有菜单（管理员）
     *
     * @return 所有菜单列表
     */
    List<MenuResponse> getAllMenus();
}