package com.aimedical.modules.commonmodule.controller;

import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.dto.request.MenuCreateRequest;
import com.aimedical.modules.commonmodule.dto.request.MenuUpdateRequest;
import com.aimedical.modules.commonmodule.dto.response.MenuResponse;
import com.aimedical.modules.commonmodule.jwt.JwtConfig;
import com.aimedical.modules.commonmodule.jwt.JwtUtil;
import com.aimedical.modules.commonmodule.service.MenuService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MenuController纯单元测试
 *
 * <p>不依赖Spring容器，直接测试Controller方法逻辑。
 * 使用真实JwtUtil实例，避免传入null导致后续维护NPE。
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MenuController测试")
class MenuControllerTest {

    @Mock
    private MenuService menuService;

    private JwtUtil jwtUtil;
    private MenuController menuController;

    private MenuResponse mockMenu;

    @BeforeEach
    void setUp() {
        // 创建真实的JwtUtil实例，避免传入null
        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setSecret("AIMedicalSysJwtSecretKey2026Phase1DevelopmentTestSecretKey");
        jwtConfig.setExpiration(86400L);
        jwtConfig.setTokenType("Bearer");
        jwtUtil = new JwtUtil(jwtConfig);

        menuController = new MenuController(menuService, jwtUtil);

        mockMenu = new MenuResponse();
        mockMenu.setId(1L);
        mockMenu.setName("测试菜单");
        mockMenu.setPath("/test");
        mockMenu.setIcon("test-icon");
        mockMenu.setSortOrder(1);
    }

    @Nested
    @DisplayName("GET /api/menu/{id}")
    class GetMenuTests {

        @Test
        @DisplayName("获取菜单详情成功")
        void shouldReturnSuccessWhenGetMenuSucceeds() {
            when(menuService.getMenuById(1L)).thenReturn(mockMenu);

            Result<MenuResponse> result = menuController.get(1L);

            assertEquals("SUCCESS", result.getCode());
            assertEquals("测试菜单", result.getData().getName());
        }

        @Test
        @DisplayName("菜单不存在返回MENU_NOT_FOUND")
        void shouldReturnNotFoundWhenMenuNotExists() {
            when(menuService.getMenuById(999L)).thenReturn(null);

            Result<MenuResponse> result = menuController.get(999L);

            assertEquals("MENU_NOT_FOUND", result.getCode());
            assertEquals("菜单不存在", result.getMessage());
        }
    }

    @Nested
    @DisplayName("POST /api/menu")
    class CreateMenuTests {

        @Test
        @DisplayName("创建菜单成功")
        void shouldReturnSuccessWhenCreateMenuSucceeds() {
            MenuCreateRequest request = new MenuCreateRequest();
            request.setName("新菜单");
            request.setCode("menu:new");
            request.setPath("/new-menu");
            request.setIcon("plus");
            request.setSortOrder(10);
            request.setVisible(true);
            request.setEnabled(true);

            when(menuService.createMenu(any(MenuCreateRequest.class))).thenReturn(mockMenu);

            Result<MenuResponse> result = menuController.create(request);

            assertEquals("SUCCESS", result.getCode());
            assertEquals("测试菜单", result.getData().getName());
            verify(menuService, times(1)).createMenu(any(MenuCreateRequest.class));
        }
    }

    @Nested
    @DisplayName("PUT /api/menu/{id}")
    class UpdateMenuTests {

        @Test
        @DisplayName("更新菜单成功")
        void shouldReturnSuccessWhenUpdateMenuSucceeds() {
            MenuUpdateRequest request = new MenuUpdateRequest();
            request.setName("更新后的菜单");
            request.setPath("/updated-menu");

            when(menuService.updateMenu(eq(1L), any(MenuUpdateRequest.class))).thenReturn(mockMenu);

            Result<MenuResponse> result = menuController.update(1L, request);

            assertEquals("SUCCESS", result.getCode());
            verify(menuService, times(1)).updateMenu(eq(1L), any(MenuUpdateRequest.class));
        }

        @Test
        @DisplayName("更新不存在的菜单返回MENU_NOT_FOUND")
        void shouldReturnNotFoundWhenUpdateNonExistentMenu() {
            MenuUpdateRequest request = new MenuUpdateRequest();
            request.setName("更新后的菜单");

            when(menuService.updateMenu(eq(999L), any(MenuUpdateRequest.class))).thenReturn(null);

            Result<MenuResponse> result = menuController.update(999L, request);

            assertEquals("MENU_NOT_FOUND", result.getCode());
            assertEquals("菜单不存在", result.getMessage());
        }
    }

    @Nested
    @DisplayName("DELETE /api/menu/{id}")
    class DeleteMenuTests {

        @Test
        @DisplayName("删除菜单成功")
        void shouldReturnSuccessWhenDeleteMenuSucceeds() {
            doNothing().when(menuService).deleteMenu(1L);

            Result<Void> result = menuController.delete(1L);

            assertEquals("SUCCESS", result.getCode());
            verify(menuService, times(1)).deleteMenu(1L);
        }
    }
}
