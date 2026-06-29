package com.aimedical.modules.commonmodule.auth.converter;

import com.aimedical.modules.commonmodule.auth.UserInfoResponse;
import com.aimedical.modules.commonmodule.permission.PermissionFunction;
import com.aimedical.modules.commonmodule.permission.Post;
import com.aimedical.modules.commonmodule.permission.Role;
import com.aimedical.modules.commonmodule.permission.User;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserConverterTest {

    private final UserConverter converter = new UserConverter();

    @Test
    void toUserInfoResponse_shouldMapBasicFields() {
        User user = new User();
        user.setId(1L);
        user.setUsername("test");
        user.setNickname("Test");
        user.setPhone("13800138000");
        user.setEmail("test@test.com");
        user.setEnabled(true);
        user.setRoles(null);
        user.setPosts(null);

        UserInfoResponse response = converter.toUserInfoResponse(user);

        assertEquals(1L, response.id());
        assertEquals("test", response.username());
        assertEquals("Test", response.realName());
        assertEquals("13800138000", response.phone());
        assertEquals("test@test.com", response.email());
        assertEquals("", response.role());
        assertEquals("", response.position());
        assertTrue(response.permissions().isEmpty());
    }

    @Test
    void toUserInfoResponse_shouldMapRoleBySortPriority() {
        User user = new User();
        user.setId(1L);

        Role adminRole = new Role();
        adminRole.setSort(2);
        adminRole.setCode("admin");

        Role doctorRole = new Role();
        doctorRole.setSort(1);
        doctorRole.setCode("doctor");

        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        roles.add(doctorRole);
        user.setRoles(roles);

        UserInfoResponse response = converter.toUserInfoResponse(user);

        assertEquals("doctor", response.role());
    }

    @Test
    void toUserInfoResponse_shouldMapRoleToEmptyWhenNoRoles() {
        User user = new User();
        user.setId(1L);
        user.setRoles(null);

        UserInfoResponse response = converter.toUserInfoResponse(user);

        assertEquals("", response.role());
    }

    @Test
    void toUserInfoResponse_shouldMapPositionFromFirstPost() {
        User user = new User();
        user.setId(1L);

        Post outpatient = new Post();
        outpatient.setCode("OUTPATIENT");

        Post inpatient = new Post();
        inpatient.setCode("INPATIENT");

        Set<Post> posts = new LinkedHashSet<>();
        posts.add(outpatient);
        posts.add(inpatient);
        user.setPosts(posts);

        UserInfoResponse response = converter.toUserInfoResponse(user);

        assertEquals("OUTPATIENT", response.position());
    }

    @Test
    void shouldFilterDisabledRole() {
        User user = new User();
        user.setId(1L);

        Role disabledRole = new Role();
        disabledRole.setEnabled(false);
        disabledRole.setSort(1);
        disabledRole.setCode("disabled_role");

        Set<Role> roles = new HashSet<>();
        roles.add(disabledRole);
        user.setRoles(roles);

        UserInfoResponse response = converter.toUserInfoResponse(user);

        assertEquals("", response.role());
    }

    @Test
    void shouldHandleNullSort() {
        User user = new User();
        user.setId(1L);

        Role role = new Role();
        role.setSort(null);
        role.setEnabled(true);
        role.setCode("doctor");

        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);

        UserInfoResponse response = converter.toUserInfoResponse(user);

        assertEquals("doctor", response.role());
    }

    @Test
    void shouldFilterDisabledPermission() {
        User user = new User();
        user.setId(1L);

        PermissionFunction disabledFunc = new PermissionFunction();
        disabledFunc.setEnabled(false);
        disabledFunc.setCode("disabled:perm");

        Set<PermissionFunction> functions = new HashSet<>();
        functions.add(disabledFunc);

        Post post = new Post();
        post.setFunctions(functions);

        Set<Post> posts = new HashSet<>();
        posts.add(post);
        user.setPosts(posts);

        UserInfoResponse response = converter.toUserInfoResponse(user);

        assertFalse(response.permissions().contains("disabled:perm"));
    }

    @Test
    void toUserInfoResponse_shouldCollectPermissions() {
        User user = new User();
        user.setId(1L);

        PermissionFunction func1 = new PermissionFunction();
        func1.setCode("menu:view");

        PermissionFunction func2 = new PermissionFunction();
        func2.setCode("user:create");

        Set<PermissionFunction> functions = new HashSet<>();
        functions.add(func1);
        functions.add(func2);

        Post post = new Post();
        post.setFunctions(functions);

        Set<Post> posts = new HashSet<>();
        posts.add(post);
        user.setPosts(posts);

        UserInfoResponse response = converter.toUserInfoResponse(user);

        assertEquals(2, response.permissions().size());
        assertTrue(response.permissions().contains("menu:view"));
        assertTrue(response.permissions().contains("user:create"));
    }

    @Test
    void shouldHandleSortNullAndDisabledRole() {
        User user = new User();
        user.setId(1L);

        Role role = new Role();
        role.setSort(null);
        role.setEnabled(false);
        role.setCode("disabled_role");

        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);

        UserInfoResponse response = converter.toUserInfoResponse(user);

        assertEquals("", response.role());
    }
}
