package com.aimedical.modules.commonmodule.controller;

import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.jwt.JwtUtil;
import com.aimedical.modules.commonmodule.dto.request.MenuCreateRequest;
import com.aimedical.modules.commonmodule.dto.request.MenuUpdateRequest;
import com.aimedical.modules.commonmodule.dto.response.MenuResponse;
import com.aimedical.modules.commonmodule.service.MenuService;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 菜单控制器
 *
 * <p>提供菜单相关的REST API接口。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final MenuService menuService;
    private final JwtUtil jwtUtil;

    public MenuController(MenuService menuService, JwtUtil jwtUtil) {
        this.menuService = menuService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 获取当前用户菜单树
     *
     * @param authHeader Authorization请求头
     * @return 菜单树列表
     */
    @GetMapping("/tree")
    public Result<List<MenuResponse>> tree(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractToken(authHeader);
        if (token == null) {
            return Result.fail("UNAUTHORIZED", "未提供令牌");
        }

        if (!jwtUtil.validateToken(token)) {
            return Result.fail("UNAUTHORIZED", "令牌无效");
        }

        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            return Result.fail("UNAUTHORIZED", "令牌无效");
        }

        List<MenuResponse> menus = menuService.getUserMenuTree(userId);
        return Result.success(menus);
    }

    /**
     * 获取所有菜单（仅管理员）
     *
     * <p>此接口通过@PreAuthorize注解进行权限控制，普通用户无法访问。
     *
     * @param authHeader Authorization请求头
     * @return 所有菜单列表
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<MenuResponse>> all(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractToken(authHeader);
        if (token == null) {
            return Result.fail("UNAUTHORIZED", "未提供令牌");
        }

        if (!jwtUtil.validateToken(token)) {
            return Result.fail("UNAUTHORIZED", "令牌无效");
        }

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
     * 从Authorization头中提取JWT令牌
     *
     * <p>使用JwtUtil统一的静态方法提取token。
     *
     * @param authHeader Authorization请求头
     * @return JWT令牌，如果无效则返回null
     */
    private String extractToken(String authHeader) {
        return JwtUtil.extractToken(authHeader, jwtUtil.getTokenType());
    }
}