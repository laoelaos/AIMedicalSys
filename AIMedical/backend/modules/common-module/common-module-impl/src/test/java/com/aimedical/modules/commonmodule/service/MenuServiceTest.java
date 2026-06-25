package com.aimedical.modules.commonmodule.service;

import com.aimedical.common.base.MenuType;
import com.aimedical.common.exception.BusinessException;
import com.aimedical.modules.commonmodule.dto.request.MenuCreateRequest;
import com.aimedical.modules.commonmodule.dto.request.MenuUpdateRequest;
import com.aimedical.modules.commonmodule.dto.response.MenuResponse;
import com.aimedical.modules.commonmodule.permission.Function;
import com.aimedical.modules.commonmodule.permission.FunctionRepository;
import com.aimedical.modules.commonmodule.permission.Post;
import com.aimedical.modules.commonmodule.permission.User;
import com.aimedical.modules.commonmodule.permission.UserRepository;
import com.aimedical.modules.commonmodule.service.impl.MenuServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * MenuService单元测试
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MenuService测试")
class MenuServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FunctionRepository functionRepository;

    private MenuService menuService;

    private User testUser;
    private Post testPost;
    private Function testFunction;

    @BeforeEach
    void setUp() {
        menuService = new MenuServiceImpl(userRepository, functionRepository);

        // 创建测试功能
        testFunction = new Function();
        testFunction.setId(1L);
        testFunction.setCode("menu:dashboard");
        testFunction.setName("仪表盘");
        testFunction.setPath("/dashboard");
        testFunction.setIcon("dashboard");
        testFunction.setEnabled(true);
        testFunction.setDeleted(false);
        testFunction.setSortOrder(0);

        // 创建测试岗位
        testPost = new Post();
        testPost.setId(1L);
        testPost.setCode("DOCTOR_GENERAL");
        testPost.setName("普通医生");
        Set<Function> functions = new HashSet<>();
        functions.add(testFunction);
        testPost.setFunctions(functions);

        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testdoctor");
        testUser.setNickname("测试医生");
        Set<Post> posts = new HashSet<>();
        posts.add(testPost);
        testUser.setPosts(posts);
    }

    @Nested
    @DisplayName("getUserMenuTree")
    class GetUserMenuTreeTests {

        @Test
        @DisplayName("获取用户菜单树成功")
        void shouldGetUserMenuTreeSuccessfully() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            List<MenuResponse> menus = menuService.getUserMenuTree(1L);

            assertNotNull(menus);
            assertEquals(1, menus.size());
            assertEquals("仪表盘", menus.get(0).getName());
            assertEquals("/dashboard", menus.get(0).getPath());
        }

        @Test
        @DisplayName("用户不存在返回空列表")
        void shouldReturnEmptyListWhenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            List<MenuResponse> menus = menuService.getUserMenuTree(999L);

            assertNotNull(menus);
            assertTrue(menus.isEmpty());
        }

        @Test
        @DisplayName("用户无岗位返回空列表")
        void shouldReturnEmptyListWhenUserHasNoPosts() {
            testUser.setPosts(null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            List<MenuResponse> menus = menuService.getUserMenuTree(1L);

            assertNotNull(menus);
            assertTrue(menus.isEmpty());
        }
    }

    @Nested
    @DisplayName("getAllMenus")
    class GetAllMenusTests {

        @Test
        @DisplayName("获取所有菜单成功")
        void shouldGetAllMenusSuccessfully() {
            Function func1 = new Function();
            func1.setId(1L);
            func1.setCode("menu:user");
            func1.setName("用户管理");
            func1.setPath("/users");
            func1.setEnabled(true);
            func1.setDeleted(false);
            func1.setSortOrder(1);

            Function func2 = new Function();
            func2.setId(2L);
            func2.setCode("menu:role");
            func2.setName("角色管理");
            func2.setPath("/roles");
            func2.setEnabled(true);
            func2.setDeleted(false);
            func2.setSortOrder(2);

            when(functionRepository.findAll()).thenReturn(Arrays.asList(func1, func2));

            List<MenuResponse> menus = menuService.getAllMenus();

            assertNotNull(menus);
            assertEquals(2, menus.size());
            assertEquals("用户管理", menus.get(0).getName());
            assertEquals("角色管理", menus.get(1).getName());
        }

        @Test
        @DisplayName("无菜单返回空列表")
        void shouldReturnEmptyListWhenNoMenus() {
            when(functionRepository.findAll()).thenReturn(Collections.emptyList());

            List<MenuResponse> menus = menuService.getAllMenus();

            assertNotNull(menus);
            assertTrue(menus.isEmpty());
        }

        @Test
        @DisplayName("deleted 过滤由 @SQLRestriction 在 SQL 层处理，Service 不再重复过滤")
        void shouldNotFilterDeletedInJavaLayer() {
            // T2/T8 修复：Service 不再在 Java 层过滤 deleted，依赖 BaseEntity 的 @SQLRestriction("deleted = false")
            // 单元测试 mock 的 findAll() 不经过 Hibernate，因此 deleted=true 的 Function 也会被返回。
            // 真实 SQL 查询时 @SQLRestriction 会自动添加 WHERE deleted = false 条件。
            Function func1 = new Function();
            func1.setId(1L);
            func1.setCode("menu:active");
            func1.setName("活跃菜单");
            func1.setEnabled(true);
            func1.setDeleted(false);
            func1.setSortOrder(1);

            Function deletedFunc = new Function();
            deletedFunc.setId(2L);
            deletedFunc.setCode("menu:deleted");
            deletedFunc.setName("已删除菜单");
            deletedFunc.setEnabled(true);
            deletedFunc.setDeleted(true);
            deletedFunc.setSortOrder(2);

            when(functionRepository.findAll()).thenReturn(Arrays.asList(func1, deletedFunc));

            List<MenuResponse> menus = menuService.getAllMenus();

            // 单元测试中 mock 返回 2 条（不经过 @SQLRestriction），Service 不再过滤
            assertEquals(2, menus.size());
        }
    }

    @Nested
    @DisplayName("createMenu")
    class CreateMenuTests {

        @Test
        @DisplayName("创建菜单成功")
        void shouldCreateMenuSuccessfully() {
            MenuCreateRequest request = new MenuCreateRequest();
            request.setName("新菜单");
            request.setCode("menu:new");
            request.setPath("/new-menu");
            request.setIcon("plus");
            request.setType(MenuType.MENU);
            request.setSortOrder(10);
            request.setVisible(true);
            request.setEnabled(true);

            Function savedFunction = new Function();
            savedFunction.setId(1L);
            savedFunction.setName("新菜单");
            savedFunction.setCode("menu:new");
            savedFunction.setPath("/new-menu");
            savedFunction.setIcon("plus");
            savedFunction.setType(MenuType.MENU.getCode());
            savedFunction.setSortOrder(10);
            savedFunction.setVisible(true);
            savedFunction.setEnabled(true);
            savedFunction.setDeleted(false);

            when(functionRepository.existsByCode("menu:new")).thenReturn(false);
            when(functionRepository.save(any(Function.class))).thenReturn(savedFunction);

            MenuResponse response = menuService.createMenu(request);

            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals("新菜单", response.getName());
            assertEquals("/new-menu", response.getPath());
            verify(functionRepository, times(1)).save(any(Function.class));
        }

        @Test
        @DisplayName("创建带父菜单的菜单成功")
        void shouldCreateMenuWithParentSuccessfully() {
            MenuCreateRequest request = new MenuCreateRequest();
            request.setName("子菜单");
            request.setCode("menu:child");
            request.setType(MenuType.MENU);
            request.setParentId(1L);

            Function parentFunction = new Function();
            parentFunction.setId(1L);
            parentFunction.setCode("menu:parent");
            parentFunction.setName("父菜单");

            Function savedFunction = new Function();
            savedFunction.setId(2L);
            savedFunction.setName("子菜单");
            savedFunction.setCode("menu:child");
            savedFunction.setParent(parentFunction);
            savedFunction.setType(MenuType.MENU.getCode());
            savedFunction.setSortOrder(0);
            savedFunction.setVisible(true);
            savedFunction.setEnabled(true);
            savedFunction.setDeleted(false);

            when(functionRepository.existsByCode("menu:child")).thenReturn(false);
            when(functionRepository.findById(1L)).thenReturn(Optional.of(parentFunction));
            when(functionRepository.save(any(Function.class))).thenReturn(savedFunction);

            MenuResponse response = menuService.createMenu(request);

            assertNotNull(response);
            assertEquals("子菜单", response.getName());
            verify(functionRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("code重复时抛出BusinessException")
        void shouldThrowBusinessExceptionWhenCodeDuplicate() {
            MenuCreateRequest request = new MenuCreateRequest();
            request.setName("重复菜单");
            request.setCode("menu:existing");
            request.setType(MenuType.MENU);

            when(functionRepository.existsByCode("menu:existing")).thenReturn(true);

            assertThrows(BusinessException.class, () -> menuService.createMenu(request));
            verify(functionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateMenu")
    class UpdateMenuTests {

        @Test
        @DisplayName("更新菜单成功")
        void shouldUpdateMenuSuccessfully() {
            MenuUpdateRequest request = new MenuUpdateRequest();
            request.setName("更新后的菜单");
            request.setPath("/updated-menu");

            Function existingFunction = new Function();
            existingFunction.setId(1L);
            existingFunction.setCode("menu:test");
            existingFunction.setName("测试菜单");
            existingFunction.setPath("/test");
            existingFunction.setSortOrder(0);
            existingFunction.setVisible(true);
            existingFunction.setEnabled(true);
            existingFunction.setDeleted(false);

            Function updatedFunction = new Function();
            updatedFunction.setId(1L);
            updatedFunction.setCode("menu:test");
            updatedFunction.setName("更新后的菜单");
            updatedFunction.setPath("/updated-menu");
            updatedFunction.setSortOrder(0);
            updatedFunction.setVisible(true);
            updatedFunction.setEnabled(true);
            updatedFunction.setDeleted(false);

            when(functionRepository.findById(1L)).thenReturn(Optional.of(existingFunction));
            when(functionRepository.save(any(Function.class))).thenReturn(updatedFunction);

            MenuResponse response = menuService.updateMenu(1L, request);

            assertNotNull(response);
            assertEquals("更新后的菜单", response.getName());
            assertEquals("/updated-menu", response.getPath());
        }

        @Test
        @DisplayName("菜单不存在返回null")
        void shouldReturnNullWhenMenuNotFound() {
            MenuUpdateRequest request = new MenuUpdateRequest();
            request.setName("更新后的菜单");

            when(functionRepository.findById(999L)).thenReturn(Optional.empty());

            MenuResponse response = menuService.updateMenu(999L, request);

            assertNull(response);
            verify(functionRepository, never()).save(any());
        }

        @Test
        @DisplayName("parentId自引用时抛出BusinessException")
        void shouldThrowBusinessExceptionWhenParentIdSelfReference() {
            MenuUpdateRequest request = new MenuUpdateRequest();
            request.setParentId(1L);

            Function existingFunction = new Function();
            existingFunction.setId(1L);
            existingFunction.setCode("menu:test");
            existingFunction.setName("测试菜单");
            existingFunction.setDeleted(false);

            when(functionRepository.findById(1L)).thenReturn(Optional.of(existingFunction));

            assertThrows(BusinessException.class, () -> menuService.updateMenu(1L, request));
            verify(functionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteMenu")
    class DeleteMenuTests {

        @Test
        @DisplayName("删除菜单成功")
        void shouldDeleteMenuSuccessfully() {
            when(functionRepository.findByParentId(1L)).thenReturn(Collections.emptyList());
            when(functionRepository.existsById(1L)).thenReturn(true);
            doNothing().when(functionRepository).deleteById(1L);

            assertDoesNotThrow(() -> menuService.deleteMenu(1L));

            verify(functionRepository, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("存在子菜单时抛出BusinessException")
        void shouldThrowBusinessExceptionWhenHasChildren() {
            Function child = new Function();
            child.setId(2L);
            child.setName("子菜单");

            when(functionRepository.findByParentId(1L)).thenReturn(Collections.singletonList(child));

            assertThrows(BusinessException.class, () -> menuService.deleteMenu(1L));
            verify(functionRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("菜单不存在时抛出BusinessException")
        void shouldThrowBusinessExceptionWhenMenuNotExists() {
            when(functionRepository.findByParentId(999L)).thenReturn(Collections.emptyList());
            when(functionRepository.existsById(999L)).thenReturn(false);

            assertThrows(BusinessException.class, () -> menuService.deleteMenu(999L));
            verify(functionRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("getMenuById")
    class GetMenuByIdTests {

        @Test
        @DisplayName("根据ID获取菜单成功")
        void shouldGetMenuByIdSuccessfully() {
            Function function = new Function();
            function.setId(1L);
            function.setCode("menu:test");
            function.setName("测试菜单");
            function.setPath("/test");
            function.setSortOrder(0);
            function.setVisible(true);
            function.setEnabled(true);
            function.setDeleted(false);

            when(functionRepository.findById(1L)).thenReturn(Optional.of(function));

            MenuResponse response = menuService.getMenuById(1L);

            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals("测试菜单", response.getName());
        }

        @Test
        @DisplayName("菜单不存在返回null")
        void shouldReturnNullWhenMenuNotFound() {
            when(functionRepository.findById(999L)).thenReturn(Optional.empty());

            MenuResponse response = menuService.getMenuById(999L);

            assertNull(response);
        }
    }
}
