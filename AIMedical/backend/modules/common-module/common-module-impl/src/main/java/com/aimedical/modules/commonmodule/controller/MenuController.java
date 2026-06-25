package com.aimedical.modules.commonmodule.controller;

import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.dto.request.MenuCreateRequest;
import com.aimedical.modules.commonmodule.dto.request.MenuUpdateRequest;
import com.aimedical.modules.commonmodule.dto.response.MenuResponse;
import com.aimedical.modules.commonmodule.service.MenuService;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 菜单控制器
 *
 * <p>提供菜单相关的REST API接口。
 *
 * <p>安全策略（T1/T7 修复后统一）：
 * <ul>
 *   <li>所有端点均通过 JwtAuthenticationFilter + @PreAuthorize 进行统一认证与授权</li>
 *   <li>不再在 Controller 层手动解析 token，避免与过滤器职责重叠</li>
 *   <li>当前用户 ID 从 SecurityContext 获取（JwtAuthenticationFilter 设置 principal=userId）</li>
 *   <li>/tree 需要登录（isAuthenticated），/all 及 CRUD 需要 ADMIN 角色</li>
 * </ul>
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    /**
     * 获取当前用户菜单树
     *
     * <p>需要登录认证（任意已认证用户均可访问自己的菜单树）。
     * 当前用户 ID 从 SecurityContext 获取，避免手动解析 token。
     *
     * @return 菜单树列表
     */
    @GetMapping("/tree")
    @PreAuthorize("isAuthenticated()")
    public Result<List<MenuResponse>> tree() {
        Long userId = getCurrentUserId();
        List<MenuResponse> menus = menuService.getUserMenuTree(userId);
        return Result.success(menus);
    }

    /**
     * 获取所有菜单（仅管理员）
     *
     * <p>此接口通过@PreAuthorize注解进行权限控制，普通用户无法访问。
     *
     * @return 所有菜单列表
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<MenuResponse>> all() {
        List<MenuResponse> menus = menuService.getAllMenus();
        return Result.success(menus);
    }

    /**
     * 获取菜单详情（仅管理员）
     *
     * @param id 菜单ID
     * @return 菜单详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<MenuResponse> get(@PathVariable Long id) {
        MenuResponse menu = menuService.getMenuById(id);
        if (menu == null) {
            return Result.fail("MENU_NOT_FOUND", "菜单不存在");
        }
        return Result.success(menu);
    }

    /**
     * 创建菜单（仅管理员）
     *
     * @param request 创建菜单请求
     * @return 创建的菜单信息
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<MenuResponse> create(@Valid @RequestBody MenuCreateRequest request) {
        MenuResponse menu = menuService.createMenu(request);
        return Result.success(menu);
    }

    /**
     * 更新菜单（仅管理员）
     *
     * @param id 菜单ID
     * @param request 更新菜单请求
     * @return 更新后的菜单信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<MenuResponse> update(@PathVariable Long id, @Valid @RequestBody MenuUpdateRequest request) {
        MenuResponse menu = menuService.updateMenu(id, request);
        if (menu == null) {
            return Result.fail("MENU_NOT_FOUND", "菜单不存在");
        }
        return Result.success(menu);
    }

    /**
     * 删除菜单（仅管理员）
     *
     * @param id 菜单ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return Result.success(null);
    }

    /**
     * 从SecurityContext获取当前登录用户ID
     *
     * <p>JwtAuthenticationFilter 将 userId 设置为 Authentication 的 principal。
     * 此处统一从 SecurityContext 获取，避免 Controller 层手动解析 token。
     *
     * @return 当前用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        if (principal instanceof Integer) {
            return ((Integer) principal).longValue();
        }
        throw new IllegalStateException("无法从SecurityContext获取用户ID");
    }
}
