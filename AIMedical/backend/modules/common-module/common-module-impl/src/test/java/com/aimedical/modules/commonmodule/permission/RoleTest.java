package com.aimedical.modules.commonmodule.permission;

import com.aimedical.common.base.BaseEntity;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        Role role = new Role();
        assertNotNull(role);
        assertInstanceOf(BaseEntity.class, role);
    }

    @Test
    void shouldSetAndGetCode() {
        Role role = new Role();
        role.setCode("ROLE_ADMIN");
        assertEquals("ROLE_ADMIN", role.getCode());
    }

    @Test
    void shouldSetAndGetName() {
        Role role = new Role();
        role.setName("管理员");
        assertEquals("管理员", role.getName());
    }

    @Test
    void shouldSetAndGetDescription() {
        Role role = new Role();
        role.setDescription("系统管理员");
        assertEquals("系统管理员", role.getDescription());
    }

    @Test
    void shouldSetAndGetEnabled() {
        Role role = new Role();
        role.setEnabled(true);
        assertTrue(role.getEnabled());
        role.setEnabled(false);
        assertFalse(role.getEnabled());
    }

    @Test
    void shouldSetAndGetPosts() {
        Role role = new Role();
        Set<Post> posts = new HashSet<>();
        posts.add(new Post());
        role.setPosts(posts);
        assertEquals(1, role.getPosts().size());
    }

    @Test
    void shouldSetAndGetUsers() {
        Role role = new Role();
        Set<User> users = new HashSet<>();
        users.add(new User());
        role.setUsers(users);
        assertEquals(1, role.getUsers().size());
    }

    @Test
    void shouldDefaultSortIsZero() {
        Role role = new Role();
        assertEquals(0, role.getSort().intValue());
    }

    @Test
    void shouldSetAndGetSort() {
        Role role = new Role();
        role.setSort(1);
        assertEquals(1, role.getSort().intValue());
        role.setSort(999);
        assertEquals(999, role.getSort().intValue());
    }

    @Test
    void shouldHaveNonNullSort() {
        Role role = new Role();
        assertNotNull(role.getSort());
    }
}
