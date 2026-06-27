package com.aimedical.modules.commonmodule.controller;

import com.aimedical.common.result.Result;
import com.aimedical.modules.commonmodule.dto.request.MenuCreateRequest;
import com.aimedical.modules.commonmodule.dto.request.MenuUpdateRequest;
import com.aimedical.modules.commonmodule.dto.response.MenuResponse;
import com.aimedical.modules.commonmodule.auth.CurrentUser;
import com.aimedical.modules.commonmodule.service.MenuService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MenuController测试")
class MenuControllerTest {

    @Mock
    private MenuService menuService;
    @Mock
    private CurrentUser currentUser;

    private MenuController menuController;

    private MenuResponse mockMenu;

    @BeforeEach
    void setUp() {
        menuController = new MenuController(menuService, currentUser);

        mockMenu = new MenuResponse(1L, "测试菜单", "/test", null,
                "test-icon", "menu:test", 1, null);
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
            assertEquals("测试菜单", result.getData().name());
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
            var request = new MenuCreateRequest("新菜单", "menu:new", null,
                    "/new-menu", null, "plus", 10, true);

            when(menuService.createMenu(any(MenuCreateRequest.class))).thenReturn(mockMenu);

            Result<MenuResponse> result = menuController.create(request);

            assertEquals("SUCCESS", result.getCode());
            assertEquals("测试菜单", result.getData().name());
            verify(menuService, times(1)).createMenu(any(MenuCreateRequest.class));
        }
    }

    @Nested
    @DisplayName("PATCH /api/menu/{id}")
    class UpdateMenuTests {

        @Test
        @DisplayName("更新菜单成功")
        void shouldReturnSuccessWhenUpdateMenuSucceeds() {
            var request = new MenuUpdateRequest();
            request.setName("更新后的菜单");
            request.setPath("/updated-menu");

            when(menuService.updateMenu(eq(1L), any(MenuUpdateRequest.class))).thenReturn(mockMenu);

            Result<MenuResponse> result = menuController.update(1L, request);

            assertEquals("SUCCESS", result.getCode());
            verify(menuService, times(1)).updateMenu(eq(1L), any(MenuUpdateRequest.class));
        }

        @Test
        @DisplayName("路径id与请求体id一致时更新成功")
        void shouldReturnSuccessWhenPathIdMatchesBodyId() {
            var request = new MenuUpdateRequest();
            request.setId(1L);
            request.setName("更新后的菜单");

            when(menuService.updateMenu(eq(1L), any(MenuUpdateRequest.class))).thenReturn(mockMenu);

            Result<MenuResponse> result = menuController.update(1L, request);

            assertEquals("SUCCESS", result.getCode());
            assertEquals("测试菜单", result.getData().name());
            verify(menuService, times(1)).updateMenu(eq(1L), any(MenuUpdateRequest.class));
        }

        @Test
        @DisplayName("更新不存在的菜单返回MENU_NOT_FOUND")
        void shouldReturnNotFoundWhenUpdateNonExistentMenu() {
            var request = new MenuUpdateRequest();
            request.setName("更新后的菜单");

            when(menuService.updateMenu(eq(999L), any(MenuUpdateRequest.class))).thenReturn(null);

            Result<MenuResponse> result = menuController.update(999L, request);

            assertEquals("MENU_NOT_FOUND", result.getCode());
            assertEquals("菜单不存在", result.getMessage());
        }
    }

    @Nested
    @DisplayName("PATCH /api/menu/{id}")
    class PathIdConsistencyTests {

        @Test
        @DisplayName("路径id与请求体id不一致返回PARAM_INVALID")
        void shouldReturnParamInvalidWhenPathIdMismatchBodyId() {
            var request = new MenuUpdateRequest();
            request.setId(2L);

            Result<MenuResponse> result = menuController.update(1L, request);

            assertEquals("PARAM_INVALID", result.getCode());
            assertEquals("参数校验失败", result.getMessage());
            verify(menuService, never()).updateMenu(any(), any());
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
