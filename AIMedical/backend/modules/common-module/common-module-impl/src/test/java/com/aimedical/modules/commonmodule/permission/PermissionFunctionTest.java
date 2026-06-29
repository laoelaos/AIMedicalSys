package com.aimedical.modules.commonmodule.permission;

import com.aimedical.common.base.BaseEntity;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PermissionFunctionTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        PermissionFunction permissionFunction = new PermissionFunction();
        assertNotNull(permissionFunction);
        assertInstanceOf(BaseEntity.class, permissionFunction);
    }

    @Test
    void shouldSetAndGetCode() {
        PermissionFunction permissionFunction = new PermissionFunction();
        permissionFunction.setCode("FUNC_CREATE");
        assertEquals("FUNC_CREATE", permissionFunction.getCode());
    }

    @Test
    void shouldSetAndGetName() {
        PermissionFunction permissionFunction = new PermissionFunction();
        permissionFunction.setName("创建");
        assertEquals("创建", permissionFunction.getName());
    }

    @Test
    void shouldSetAndGetDescription() {
        PermissionFunction permissionFunction = new PermissionFunction();
        permissionFunction.setDescription("创建操作");
        assertEquals("创建操作", permissionFunction.getDescription());
    }

    @Test
    void shouldSetAndGetEnabled() {
        PermissionFunction permissionFunction = new PermissionFunction();
        permissionFunction.setEnabled(true);
        assertTrue(permissionFunction.getEnabled());
        permissionFunction.setEnabled(false);
        assertFalse(permissionFunction.getEnabled());
    }

    @Test
    void shouldSetAndGetPosts() {
        PermissionFunction permissionFunction = new PermissionFunction();
        Set<Post> posts = new HashSet<>();
        posts.add(new Post());
        permissionFunction.setPosts(posts);
        assertEquals(1, permissionFunction.getPosts().size());
    }

    @Test
    void shouldSetAndGetComponent() {
        PermissionFunction permissionFunction = new PermissionFunction();
        permissionFunction.setComponent("DashboardComp");
        assertEquals("DashboardComp", permissionFunction.getComponent());
    }
}
