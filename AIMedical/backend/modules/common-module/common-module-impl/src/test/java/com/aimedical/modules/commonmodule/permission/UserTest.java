package com.aimedical.modules.commonmodule.permission;

import com.aimedical.common.base.BaseEntity;
import com.aimedical.modules.commonmodule.api.UserType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        User user = new User();
        assertNotNull(user);
        assertInstanceOf(BaseEntity.class, user);
    }

    @Test
    void shouldSetAndGetUsername() {
        User user = new User();
        user.setUsername("zhangsan");
        assertEquals("zhangsan", user.getUsername());
    }

    @Test
    void shouldSetAndGetPassword() {
        User user = new User();
        user.setPassword("pwd123");
        assertEquals("pwd123", user.getPassword());
    }

    @Test
    void shouldSetAndGetNickname() {
        User user = new User();
        user.setNickname("张三");
        assertEquals("张三", user.getNickname());
    }

    @Test
    void shouldSetAndGetPhone() {
        User user = new User();
        user.setPhone("13800138000");
        assertEquals("13800138000", user.getPhone());
    }

    @Test
    void shouldSetAndGetEmail() {
        User user = new User();
        user.setEmail("test@test.com");
        assertEquals("test@test.com", user.getEmail());
    }

    @Test
    void shouldSetAndGetEnabled() {
        User user = new User();
        user.setEnabled(true);
        assertTrue(user.getEnabled());
        user.setEnabled(false);
        assertFalse(user.getEnabled());
    }

    @Test
    void shouldSetAndGetUserType() {
        User user = new User();
        user.setUserType(UserType.DOCTOR);
        assertEquals(UserType.DOCTOR, user.getUserType());
    }

    @Test
    void shouldSetAndGetRoles() {
        User user = new User();
        Set<Role> roles = new HashSet<>();
        roles.add(new Role());
        user.setRoles(roles);
        assertEquals(1, user.getRoles().size());
    }

    @Test
    void shouldSetAndGetPosts() {
        User user = new User();
        Set<Post> posts = new HashSet<>();
        posts.add(new Post());
        user.setPosts(posts);
        assertEquals(1, user.getPosts().size());
    }

    @Test
    void shouldBeAbleToAddMultipleRoles() {
        User user = new User();
        Set<Role> roles = new HashSet<>();
        roles.add(new Role());
        roles.add(new Role());
        user.setRoles(roles);
        assertEquals(2, user.getRoles().size());
    }

    @Test
    void shouldDefaultPasswordChangeRequiredIsFalse() {
        User user = new User();
        assertFalse(user.getPasswordChangeRequired());
    }

    @Test
    void shouldSetAndGetPasswordChangeRequired() {
        User user = new User();
        user.setPasswordChangeRequired(true);
        assertTrue(user.getPasswordChangeRequired());
        user.setPasswordChangeRequired(false);
        assertFalse(user.getPasswordChangeRequired());
    }

    @Test
    void shouldDefaultTokenVersionIsZero() {
        User user = new User();
        assertEquals(0, user.getTokenVersion().intValue());
    }

    @Test
    void shouldSetAndGetTokenVersion() {
        User user = new User();
        user.setTokenVersion(5);
        assertEquals(5, user.getTokenVersion().intValue());
        user.setTokenVersion(100);
        assertEquals(100, user.getTokenVersion().intValue());
    }
}
