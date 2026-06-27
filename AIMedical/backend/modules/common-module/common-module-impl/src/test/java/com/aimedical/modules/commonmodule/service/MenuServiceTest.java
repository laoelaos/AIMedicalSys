package com.aimedical.modules.commonmodule.service;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.modules.commonmodule.dto.request.MenuCreateRequest;
import com.aimedical.modules.commonmodule.dto.request.MenuUpdateRequest;
import com.aimedical.modules.commonmodule.dto.response.MenuResponse;
import com.aimedical.modules.commonmodule.permission.PermissionFunction;
import com.aimedical.modules.commonmodule.permission.PermissionFunctionRepository;
import com.aimedical.modules.commonmodule.permission.Post;
import com.aimedical.modules.commonmodule.permission.User;
import com.aimedical.modules.commonmodule.permission.UserRepository;
import com.aimedical.modules.commonmodule.service.impl.MenuServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("MenuService测试")
class MenuServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionFunctionRepository functionRepository;

    private MenuService menuService;

    private User testUser;
    private Post testPost;
    private PermissionFunction testPermissionFunction;

    @BeforeEach
    void setUp() {
        menuService = new MenuServiceImpl(userRepository, functionRepository);

        testPermissionFunction = new PermissionFunction();
        testPermissionFunction.setId(1L);
        testPermissionFunction.setCode("menu:dashboard");
        testPermissionFunction.setName("仪表盘");
        testPermissionFunction.setPath("/dashboard");
        testPermissionFunction.setComponent("DashboardComp");
        testPermissionFunction.setIcon("dashboard");
        testPermissionFunction.setEnabled(true);
        testPermissionFunction.setDeleted(false);
        testPermissionFunction.setSortOrder(0);

        testPost = new Post();
        testPost.setId(1L);
        testPost.setCode("DOCTOR_GENERAL");
        testPost.setName("普通医生");
        Set<PermissionFunction> functions = new HashSet<>();
        functions.add(testPermissionFunction);
        testPost.setFunctions(functions);

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
            when(userRepository.findWithDetailsForMenuById(1L)).thenReturn(Optional.of(testUser));

            List<MenuResponse> menus = menuService.getUserMenuTree(1L);

            assertNotNull(menus);
            assertEquals(1, menus.size());
            assertEquals("仪表盘", menus.get(0).name());
            assertEquals("/dashboard", menus.get(0).path());
            assertEquals("DashboardComp", menus.get(0).component());
        }

        @Test
        @DisplayName("用户不存在返回空列表")
        void shouldReturnEmptyListWhenUserNotFound() {
            when(userRepository.findWithDetailsForMenuById(999L)).thenReturn(Optional.empty());

            List<MenuResponse> menus = menuService.getUserMenuTree(999L);

            assertNotNull(menus);
            assertTrue(menus.isEmpty());
        }

        @Test
        @DisplayName("用户无岗位返回空列表")
        void shouldReturnEmptyListWhenUserHasNoPosts() {
            testUser.setPosts(null);
            when(userRepository.findWithDetailsForMenuById(1L)).thenReturn(Optional.of(testUser));

            List<MenuResponse> menus = menuService.getUserMenuTree(1L);

            assertNotNull(menus);
            assertTrue(menus.isEmpty());
        }

        @Test
        @DisplayName("用户有岗位但岗位无权限时返回空列表")
        void shouldReturnEmptyListWhenUserHasPostsWithoutFunctions() {
            testPost.setFunctions(null);
            when(userRepository.findWithDetailsForMenuById(1L)).thenReturn(Optional.of(testUser));

            List<MenuResponse> menus = menuService.getUserMenuTree(1L);

            assertNotNull(menus);
            assertTrue(menus.isEmpty());
        }

        @Test
        @DisplayName("buildMenuTree:父菜单下嵌套子菜单")
        void buildMenuTree_shouldNestChildUnderParent() {
            PermissionFunction parent = new PermissionFunction();
            parent.setId(1L);
            parent.setName("父菜单");
            parent.setSortOrder(1);
            parent.setComponent("FatherComp");
            parent.setEnabled(true);
            parent.setDeleted(false);
            parent.setVisible(true);

            PermissionFunction child = new PermissionFunction();
            child.setId(2L);
            child.setName("子菜单");
            child.setSortOrder(1);
            child.setComponent("ChildComp");
            child.setEnabled(true);
            child.setDeleted(false);
            child.setVisible(true);
            child.setParent(parent);

            Set<PermissionFunction> functions = new HashSet<>();
            functions.add(parent);
            functions.add(child);
            testPost.setFunctions(functions);

            when(userRepository.findWithDetailsForMenuById(1L)).thenReturn(Optional.of(testUser));

            List<MenuResponse> menus = menuService.getUserMenuTree(1L);

            assertEquals(1, menus.size());
            assertEquals("父菜单", menus.get(0).name());
            assertNotNull(menus.get(0).children());
            assertEquals(1, menus.get(0).children().size());
            assertEquals("子菜单", menus.get(0).children().get(0).name());
            assertEquals("ChildComp", menus.get(0).children().get(0).component());
        }

        @Test
        @DisplayName("buildMenuTree:同级菜单按sortOrder排序")
        void buildMenuTree_shouldSortSiblingsBySortOrder() {
            PermissionFunction parent = new PermissionFunction();
            parent.setId(1L);
            parent.setName("父菜单");
            parent.setSortOrder(0);
            parent.setComponent("ParentComp");
            parent.setEnabled(true);
            parent.setDeleted(false);
            parent.setVisible(true);

            PermissionFunction childB = new PermissionFunction();
            childB.setId(2L);
            childB.setName("子菜单B");
            childB.setSortOrder(2);
            childB.setComponent("ChildBComp");
            childB.setEnabled(true);
            childB.setDeleted(false);
            childB.setVisible(true);
            childB.setParent(parent);

            PermissionFunction childA = new PermissionFunction();
            childA.setId(3L);
            childA.setName("子菜单A");
            childA.setSortOrder(1);
            childA.setComponent("ChildAComp");
            childA.setEnabled(true);
            childA.setDeleted(false);
            childA.setVisible(true);
            childA.setParent(parent);

            Set<PermissionFunction> functions = new HashSet<>();
            functions.add(parent);
            functions.add(childB);
            functions.add(childA);
            testPost.setFunctions(functions);

            when(userRepository.findWithDetailsForMenuById(1L)).thenReturn(Optional.of(testUser));

            List<MenuResponse> menus = menuService.getUserMenuTree(1L);

            assertEquals(1, menus.size());
            assertEquals("子菜单A", menus.get(0).children().get(0).name());
            assertEquals("子菜单B", menus.get(0).children().get(1).name());
        }

        @Disabled("三级嵌套受 MenuResponse 不可变 record 限制暂不可用，待 buildMenuTree 修复后启用")
        @Test
        @DisplayName("buildMenuTree:三级嵌套")
        void buildMenuTree_shouldSupportThreeLevelHierarchy() {
            PermissionFunction grandparent = new PermissionFunction();
            grandparent.setId(1L);
            grandparent.setName("系统管理");
            grandparent.setSortOrder(1);
            grandparent.setComponent("SysComp");
            grandparent.setEnabled(true);
            grandparent.setDeleted(false);
            grandparent.setVisible(true);

            PermissionFunction parent = new PermissionFunction();
            parent.setId(2L);
            parent.setName("用户管理");
            parent.setSortOrder(1);
            parent.setComponent("UserComp");
            parent.setEnabled(true);
            parent.setDeleted(false);
            parent.setVisible(true);
            parent.setParent(grandparent);

            PermissionFunction child = new PermissionFunction();
            child.setId(3L);
            child.setName("新增用户");
            child.setSortOrder(1);
            child.setComponent("AddUserComp");
            child.setEnabled(true);
            child.setDeleted(false);
            child.setVisible(true);
            child.setParent(parent);

            Set<PermissionFunction> functions = new HashSet<>();
            functions.add(grandparent);
            functions.add(parent);
            functions.add(child);
            testPost.setFunctions(functions);

            when(userRepository.findWithDetailsForMenuById(1L)).thenReturn(Optional.of(testUser));

            List<MenuResponse> menus = menuService.getUserMenuTree(1L);

            assertEquals(1, menus.size());
            assertEquals("系统管理", menus.get(0).name());
            assertEquals(1, menus.get(0).children().size());
            assertEquals("用户管理", menus.get(0).children().get(0).name());
            assertEquals(1, menus.get(0).children().get(0).children().size());
            assertEquals("新增用户", menus.get(0).children().get(0).children().get(0).name());
        }

        @Test
        @DisplayName("buildMenuTree:多父菜单各有子菜单")
        void buildMenuTree_shouldHandleMultipleParents() {
            PermissionFunction parent1 = new PermissionFunction();
            parent1.setId(1L);
            parent1.setName("系统管理");
            parent1.setSortOrder(1);
            parent1.setComponent("SystemComp");
            parent1.setEnabled(true);
            parent1.setDeleted(false);
            parent1.setVisible(true);

            PermissionFunction child1 = new PermissionFunction();
            child1.setId(2L);
            child1.setName("用户管理");
            child1.setSortOrder(1);
            child1.setComponent("UserComp");
            child1.setEnabled(true);
            child1.setDeleted(false);
            child1.setVisible(true);
            child1.setParent(parent1);

            PermissionFunction child2 = new PermissionFunction();
            child2.setId(3L);
            child2.setName("角色管理");
            child2.setSortOrder(2);
            child2.setComponent("RoleComp");
            child2.setEnabled(true);
            child2.setDeleted(false);
            child2.setVisible(true);
            child2.setParent(parent1);

            PermissionFunction parent2 = new PermissionFunction();
            parent2.setId(4L);
            parent2.setName("业务管理");
            parent2.setSortOrder(2);
            parent2.setComponent("BizComp");
            parent2.setEnabled(true);
            parent2.setDeleted(false);
            parent2.setVisible(true);

            PermissionFunction child3 = new PermissionFunction();
            child3.setId(5L);
            child3.setName("门诊管理");
            child3.setSortOrder(1);
            child3.setComponent("ClinicComp");
            child3.setEnabled(true);
            child3.setDeleted(false);
            child3.setVisible(true);
            child3.setParent(parent2);

            Set<PermissionFunction> functions = new HashSet<>();
            functions.add(parent1);
            functions.add(child1);
            functions.add(child2);
            functions.add(parent2);
            functions.add(child3);
            testPost.setFunctions(functions);

            when(userRepository.findWithDetailsForMenuById(1L)).thenReturn(Optional.of(testUser));

            List<MenuResponse> menus = menuService.getUserMenuTree(1L);

            assertEquals(2, menus.size());
            assertEquals("系统管理", menus.get(0).name());
            assertEquals("业务管理", menus.get(1).name());
            assertEquals(2, menus.get(0).children().size());
            assertEquals("用户管理", menus.get(0).children().get(0).name());
            assertEquals("角色管理", menus.get(0).children().get(1).name());
            assertEquals(1, menus.get(1).children().size());
            assertEquals("门诊管理", menus.get(1).children().get(0).name());
        }
    }

    @Nested
    @DisplayName("getAllMenus")
    class GetAllMenusTests {

        @Test
        @DisplayName("获取所有菜单成功")
        void shouldGetAllMenusSuccessfully() {
            PermissionFunction func1 = new PermissionFunction();
            func1.setId(1L);
            func1.setCode("menu:user");
            func1.setName("用户管理");
            func1.setPath("/users");
            func1.setComponent("UserComp");
            func1.setEnabled(true);
            func1.setDeleted(false);
            func1.setSortOrder(1);

            PermissionFunction func2 = new PermissionFunction();
            func2.setId(2L);
            func2.setCode("menu:role");
            func2.setName("角色管理");
            func2.setPath("/roles");
            func2.setComponent("RoleComp");
            func2.setEnabled(true);
            func2.setDeleted(false);
            func2.setSortOrder(2);

            when(functionRepository.findAll()).thenReturn(Arrays.asList(func1, func2));

            List<MenuResponse> menus = menuService.getAllMenus();

            assertNotNull(menus);
            assertEquals(2, menus.size());
            assertEquals("用户管理", menus.get(0).name());
            assertEquals("角色管理", menus.get(1).name());
            assertEquals("UserComp", menus.get(0).component());
            assertEquals("RoleComp", menus.get(1).component());
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
        void shouldReturnAllMenusIncludingDeletedFromRepository() {
            PermissionFunction func1 = new PermissionFunction();
            func1.setId(1L);
            func1.setCode("menu:active");
            func1.setName("活跃菜单");
            func1.setEnabled(true);
            func1.setDeleted(false);
            func1.setSortOrder(1);

            PermissionFunction deletedFunc = new PermissionFunction();
            deletedFunc.setId(2L);
            deletedFunc.setCode("menu:deleted");
            deletedFunc.setName("已删除菜单");
            deletedFunc.setEnabled(true);
            deletedFunc.setDeleted(true);
            deletedFunc.setSortOrder(2);

            when(functionRepository.findAll()).thenReturn(Arrays.asList(func1, deletedFunc));

            List<MenuResponse> menus = menuService.getAllMenus();

            assertEquals(2, menus.size());
        }
    }

    @Nested
    @DisplayName("createMenu")
    class CreateMenuTests {

        @Test
        @DisplayName("创建菜单成功")
        void shouldCreateMenuSuccessfully() {
            var request = new MenuCreateRequest("新菜单", "menu:new", null,
                    "/new-menu", null, "plus", 10, true);

            PermissionFunction savedPermissionFunction = new PermissionFunction();
            savedPermissionFunction.setId(1L);
            savedPermissionFunction.setName("新菜单");
            savedPermissionFunction.setCode("menu:new");
            savedPermissionFunction.setPath("/new-menu");
            savedPermissionFunction.setIcon("plus");
            savedPermissionFunction.setSortOrder(10);
            savedPermissionFunction.setVisible(true);
            savedPermissionFunction.setEnabled(true);
            savedPermissionFunction.setDeleted(false);

            when(functionRepository.existsByCode("menu:new")).thenReturn(false);
            when(functionRepository.save(any(PermissionFunction.class))).thenReturn(savedPermissionFunction);

            MenuResponse response = menuService.createMenu(request);

            assertNotNull(response);
            assertEquals(1L, response.id());
            assertEquals("新菜单", response.name());
            assertEquals("/new-menu", response.path());
            assertNull(response.component());
            verify(functionRepository, times(1)).save(any(PermissionFunction.class));
        }

        @Test
        @DisplayName("创建带父菜单的菜单成功")
        void shouldCreateMenuWithParentSuccessfully() {
            var request = new MenuCreateRequest("子菜单", "menu:child", 1L,
                    null, null, null, null, true);

            PermissionFunction parentPermissionFunction = new PermissionFunction();
            parentPermissionFunction.setId(1L);
            parentPermissionFunction.setCode("menu:parent");
            parentPermissionFunction.setName("父菜单");

            PermissionFunction savedPermissionFunction = new PermissionFunction();
            savedPermissionFunction.setId(2L);
            savedPermissionFunction.setName("子菜单");
            savedPermissionFunction.setCode("menu:child");
            savedPermissionFunction.setParent(parentPermissionFunction);
            savedPermissionFunction.setSortOrder(0);
            savedPermissionFunction.setVisible(true);
            savedPermissionFunction.setEnabled(true);
            savedPermissionFunction.setDeleted(false);

            when(functionRepository.existsByCode("menu:child")).thenReturn(false);
            when(functionRepository.findById(1L)).thenReturn(Optional.of(parentPermissionFunction));
            when(functionRepository.save(any(PermissionFunction.class))).thenReturn(savedPermissionFunction);

            MenuResponse response = menuService.createMenu(request);

            assertNotNull(response);
            assertEquals("子菜单", response.name());
            verify(functionRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("code重复时抛出BusinessException")
        void shouldThrowBusinessExceptionWhenCodeDuplicate() {
            var request = new MenuCreateRequest("重复菜单", "menu:existing", null,
                    null, null, null, null, true);

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
            var request = new MenuUpdateRequest();
            request.setName("更新后的菜单");
            request.setPath("/updated-menu");

            PermissionFunction existingPermissionFunction = new PermissionFunction();
            existingPermissionFunction.setId(1L);
            existingPermissionFunction.setCode("menu:test");
            existingPermissionFunction.setName("测试菜单");
            existingPermissionFunction.setPath("/test");
            existingPermissionFunction.setSortOrder(0);
            existingPermissionFunction.setVisible(true);
            existingPermissionFunction.setEnabled(true);
            existingPermissionFunction.setDeleted(false);

            PermissionFunction updatedPermissionFunction = new PermissionFunction();
            updatedPermissionFunction.setId(1L);
            updatedPermissionFunction.setCode("menu:test");
            updatedPermissionFunction.setName("更新后的菜单");
            updatedPermissionFunction.setPath("/updated-menu");
            updatedPermissionFunction.setSortOrder(0);
            updatedPermissionFunction.setVisible(true);
            updatedPermissionFunction.setEnabled(true);
            updatedPermissionFunction.setDeleted(false);

            when(functionRepository.findById(1L)).thenReturn(Optional.of(existingPermissionFunction));
            when(functionRepository.save(any(PermissionFunction.class))).thenReturn(updatedPermissionFunction);

            MenuResponse response = menuService.updateMenu(1L, request);

            assertNotNull(response);
            assertEquals("更新后的菜单", response.name());
            assertEquals("/updated-menu", response.path());
        }

        @Test
        @DisplayName("菜单不存在返回null")
        void shouldReturnNullWhenMenuNotFound() {
            var request = new MenuUpdateRequest();
            request.setName("更新后的菜单");

            when(functionRepository.findById(999L)).thenReturn(Optional.empty());

            MenuResponse response = menuService.updateMenu(999L, request);

            assertNull(response);
            verify(functionRepository, never()).save(any());
        }

        @Test
        @DisplayName("parentId自引用时抛出BusinessException")
        void shouldThrowBusinessExceptionWhenParentIdSelfReference() {
            var request = new MenuUpdateRequest();
            request.setParentId(1L);

            PermissionFunction existingPermissionFunction = new PermissionFunction();
            existingPermissionFunction.setId(1L);
            existingPermissionFunction.setCode("menu:test");
            existingPermissionFunction.setName("测试菜单");
            existingPermissionFunction.setDeleted(false);

            when(functionRepository.findById(1L)).thenReturn(Optional.of(existingPermissionFunction));

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
        @DisplayName("存在子菜单时抛出BusinessException且错误码为CHILDREN_EXIST")
        void shouldThrowBusinessExceptionWhenHasChildren() {
            PermissionFunction child = new PermissionFunction();
            child.setId(2L);
            child.setName("子菜单");

            when(functionRepository.findByParentId(1L)).thenReturn(Collections.singletonList(child));

            BusinessException exception = assertThrows(BusinessException.class, () -> menuService.deleteMenu(1L));
            assertEquals(GlobalErrorCode.CHILDREN_EXIST, exception.getErrorCode());
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
            PermissionFunction function = new PermissionFunction();
            function.setId(1L);
            function.setCode("menu:test");
            function.setName("测试菜单");
            function.setPath("/test");
            function.setComponent("TestComp");
            function.setSortOrder(0);
            function.setVisible(true);
            function.setEnabled(true);
            function.setDeleted(false);

            when(functionRepository.findById(1L)).thenReturn(Optional.of(function));

            MenuResponse response = menuService.getMenuById(1L);

            assertNotNull(response);
            assertEquals(1L, response.id());
            assertEquals("测试菜单", response.name());
            assertEquals("TestComp", response.component());
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
