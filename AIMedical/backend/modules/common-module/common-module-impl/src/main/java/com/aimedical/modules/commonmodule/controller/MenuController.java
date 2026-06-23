package com.aimedical.modules.commonmodule.controller;

import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.api.JwtUtil;
import com.aimedical.modules.commonmodule.dto.response.MenuResponse;
import com.aimedical.modules.commonmodule.service.MenuService;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
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

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
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

        if (!JwtUtil.validateToken(token)) {
            return Result.fail("UNAUTHORIZED", "令牌无效");
        }

        Long userId = JwtUtil.getUserId(token);
        List<MenuResponse> menus = menuService.getUserMenuTree(userId);
        return Result.success(menus);
    }

    /**
     * 获取所有菜单（管理员）
     *
     * @return 所有菜单列表
     */
    @GetMapping("/all")
    public Result<List<MenuResponse>> all() {
        List<MenuResponse> menus = menuService.getAllMenus();
        return Result.success(menus);
    }

    /**
     * 从Authorization头中提取JWT令牌
     *
     * @param authHeader Authorization请求头
     * @return JWT令牌，如果无效则返回null
     */
    private String extractToken(String authHeader) {
        if (authHeader == null || authHeader.isEmpty()) {
            return null;
        }
        String tokenType = JwtUtil.getTokenType();
        if (authHeader.startsWith(tokenType + " ")) {
            return authHeader.substring(tokenType.length() + 1);
        }
        return authHeader;
    }
}