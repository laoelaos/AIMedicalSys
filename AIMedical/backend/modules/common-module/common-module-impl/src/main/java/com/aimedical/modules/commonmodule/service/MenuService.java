package com.aimedical.modules.commonmodule.service;

import com.aimedical.modules.commonmodule.dto.request.MenuCreateRequest;
import com.aimedical.modules.commonmodule.dto.request.MenuUpdateRequest;
import com.aimedical.modules.commonmodule.dto.response.MenuResponse;

import java.util.List;

/**
 * 菜单服务接口
 *
 * <p>提供菜单相关的业务操作，包括获取用户菜单树、获取所有菜单及菜单CRUD操作。
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

    /**
     * 创建菜单
     *
     * @param request 创建菜单请求
     * @return 创建的菜单信息
     */
    MenuResponse createMenu(MenuCreateRequest request);

    /**
     * 更新菜单
     *
     * @param id 菜单ID
     * @param request 更新菜单请求
     * @return 更新后的菜单信息
     */
    MenuResponse updateMenu(Long id, MenuUpdateRequest request);

    /**
     * 删除菜单
     *
     * @param id 菜单ID
     */
    void deleteMenu(Long id);

    /**
     * 根据ID获取菜单
     *
     * @param id 菜单ID
     * @return 菜单信息，如果不存在返回null
     */
    MenuResponse getMenuById(Long id);
}