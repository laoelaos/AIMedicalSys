package com.aimedical.modules.commonmodule.permission;

import com.aimedical.common.base.BaseEntity;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PostTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        Post post = new Post();
        assertNotNull(post);
        assertInstanceOf(BaseEntity.class, post);
    }

    @Test
    void shouldSetAndGetCode() {
        Post post = new Post();
        post.setCode("POST_DEV");
        assertEquals("POST_DEV", post.getCode());
    }

    @Test
    void shouldSetAndGetName() {
        Post post = new Post();
        post.setName("开发岗");
        assertEquals("开发岗", post.getName());
    }

    @Test
    void shouldSetAndGetDescription() {
        Post post = new Post();
        post.setDescription("开发岗位");
        assertEquals("开发岗位", post.getDescription());
    }

    @Test
    void shouldSetAndGetEnabled() {
        Post post = new Post();
        post.setEnabled(true);
        assertTrue(post.getEnabled());
        post.setEnabled(false);
        assertFalse(post.getEnabled());
    }

    @Test
    void shouldSetAndGetRole() {
        Post post = new Post();
        Role role = new Role();
        post.setRole(role);
        assertSame(role, post.getRole());
    }

    @Test
    void shouldSetAndGetFunctions() {
        Post post = new Post();
        Set<Function> functions = new HashSet<>();
        functions.add(new Function());
        post.setFunctions(functions);
        assertEquals(1, post.getFunctions().size());
    }

    @Test
    void shouldSetAndGetUsers() {
        Post post = new Post();
        Set<User> users = new HashSet<>();
        users.add(new User());
        post.setUsers(users);
        assertEquals(1, post.getUsers().size());
    }
}
