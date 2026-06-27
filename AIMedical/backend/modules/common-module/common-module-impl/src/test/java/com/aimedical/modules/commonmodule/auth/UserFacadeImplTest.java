package com.aimedical.modules.commonmodule.auth;

import com.aimedical.modules.commonmodule.auth.converter.UserConverter;
import com.aimedical.modules.commonmodule.permission.PermissionFunction;
import com.aimedical.modules.commonmodule.permission.Post;
import com.aimedical.modules.commonmodule.permission.Role;
import com.aimedical.modules.commonmodule.permission.User;
import com.aimedical.modules.commonmodule.permission.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import org.springframework.dao.DataAccessException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("UserFacadeImpl")
class UserFacadeImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserConverter userConverter = new UserConverter();
    private final UserFacadeImpl userFacade = new UserFacadeImpl(userRepository, userConverter);

    @Test
    @DisplayName("findById 用户存在时返回完整 UserInfoResponse")
    void findById_whenUserExists_shouldReturnUserInfo() {
        var function = mock(PermissionFunction.class);
        when(function.getCode()).thenReturn("patient:view");
        when(function.getEnabled()).thenReturn(true);

        var post = mock(Post.class);
        when(post.getCode()).thenReturn("OUTPATIENT");
        when(post.getFunctions()).thenReturn(Set.of(function));

        var role = mock(Role.class);
        when(role.getCode()).thenReturn("DOCTOR");
        when(role.getSort()).thenReturn(0);
        when(role.getEnabled()).thenReturn(true);
        when(role.getPosts()).thenReturn(Set.of(post));

        var user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getUsername()).thenReturn("doctor001");
        when(user.getNickname()).thenReturn("张医生");
        when(user.getPhone()).thenReturn("13800138000");
        when(user.getEmail()).thenReturn("doctor@example.com");
        when(user.getRoles()).thenReturn(Set.of(role));
        when(user.getPosts()).thenReturn(Set.of(post));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        var result = userFacade.findById(1L);

        assertNotNull(result);
        assertEquals("doctor001", result.username());
        assertEquals("张医生", result.realName());
        assertEquals("DOCTOR", result.role());
        assertEquals("OUTPATIENT", result.position());
        assertTrue(result.permissions().contains("patient:view"));
    }

    @Test
    @DisplayName("findById 用户不存在时返回 null")
    void findById_whenUserNotFound_shouldReturnNull() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        var result = userFacade.findById(999L);

        assertNull(result);
    }

    @Test
    @DisplayName("findByUsername 用户存在时返回完整 UserInfoResponse")
    void findByUsername_whenUserExists_shouldReturnUserInfo() {
        var function = mock(PermissionFunction.class);
        when(function.getCode()).thenReturn("patient:view");
        when(function.getEnabled()).thenReturn(true);

        var post = mock(Post.class);
        when(post.getCode()).thenReturn("OUTPATIENT");
        when(post.getFunctions()).thenReturn(Set.of(function));

        var role = mock(Role.class);
        when(role.getCode()).thenReturn("DOCTOR");
        when(role.getSort()).thenReturn(0);
        when(role.getEnabled()).thenReturn(true);
        when(role.getPosts()).thenReturn(Set.of(post));

        var user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getUsername()).thenReturn("doctor001");
        when(user.getNickname()).thenReturn("张医生");
        when(user.getPhone()).thenReturn("13800138000");
        when(user.getEmail()).thenReturn("doctor@example.com");
        when(user.getRoles()).thenReturn(Set.of(role));
        when(user.getPosts()).thenReturn(Set.of(post));

        when(userRepository.findByUsername("doctor001")).thenReturn(Optional.of(user));

        var result = userFacade.findByUsername("doctor001");

        assertNotNull(result);
        assertEquals("doctor001", result.username());
        assertEquals("张医生", result.realName());
        assertEquals("DOCTOR", result.role());
        assertEquals("OUTPATIENT", result.position());
        assertTrue(result.permissions().contains("patient:view"));
    }

    @Test
    @DisplayName("findByUsername 用户不存在时返回 null")
    void findByUsername_whenUserNotFound_shouldReturnNull() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        var result = userFacade.findByUsername("nobody");

        assertNull(result);
    }

    @Test
    @DisplayName("existsById 用户存在时返回 true")
    void existsById_whenUserExists_shouldReturnTrue() {
        when(userRepository.existsById(1L)).thenReturn(true);

        var result = userFacade.existsById(1L);

        assertTrue(result);
    }

    @Test
    @DisplayName("existsById 用户不存在时返回 false")
    void existsById_whenUserNotFound_shouldReturnFalse() {
        when(userRepository.existsById(999L)).thenReturn(false);

        var result = userFacade.existsById(999L);

        assertFalse(result);
    }

    @Test
    @DisplayName("findById 传入 null 时返回 null")
    void findById_nullInput_shouldReturnNull() {
        when(userRepository.findById(null)).thenReturn(Optional.empty());

        var result = userFacade.findById(null);

        assertNull(result);
    }

    @Test
    @DisplayName("findByUsername 传入 null 时返回 null")
    void findByUsername_nullInput_shouldReturnNull() {
        when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

        var result = userFacade.findByUsername(null);

        assertNull(result);
    }

    @Test
    @DisplayName("existsById 传入 null 时返回 false")
    void existsById_nullInput_shouldReturnFalse() {
        when(userRepository.existsById(null)).thenReturn(false);

        var result = userFacade.existsById(null);

        assertFalse(result);
    }

    @Test
    @DisplayName("findById 用户无角色时 role 为空字符串")
    void findById_whenUserHasNoRoles_shouldReturnEmptyRole() {
        var user = mock(User.class);
        when(user.getId()).thenReturn(2L);
        when(user.getUsername()).thenReturn("nurse001");
        when(user.getNickname()).thenReturn("李护士");
        when(user.getPhone()).thenReturn("13900139000");
        when(user.getEmail()).thenReturn("nurse@example.com");
        when(user.getRoles()).thenReturn(Set.of());
        when(user.getPosts()).thenReturn(Set.of());

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        var result = userFacade.findById(2L);

        assertNotNull(result);
        assertEquals("", result.role());
        assertEquals("", result.position());
        assertTrue(result.permissions().isEmpty());
    }

    @Test
    @DisplayName("findById 用户角色全部禁用时 role 为空字符串")
    void findById_whenAllRolesDisabled_shouldReturnEmptyRole() {
        var role = mock(Role.class);
        when(role.getEnabled()).thenReturn(false);
        when(role.getSort()).thenReturn(0);
        when(role.getCode()).thenReturn("DISABLED_ROLE");

        var user = mock(User.class);
        when(user.getId()).thenReturn(3L);
        when(user.getUsername()).thenReturn("disabledroleuser");
        when(user.getNickname()).thenReturn("角色禁用用户");
        when(user.getPhone()).thenReturn((String) null);
        when(user.getEmail()).thenReturn((String) null);
        when(user.getRoles()).thenReturn(Set.of(role));
        when(user.getPosts()).thenReturn(Set.of());

        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        var result = userFacade.findById(3L);

        assertNotNull(result);
        assertEquals("", result.role());
        assertNull(result.phone());
        assertNull(result.email());
    }

    @Test
    @DisplayName("findById permissions 从 roles 和 posts 级联合并去重")
    void findById_shouldMergePermissionsFromRolesAndPosts() {
        var func1 = mock(PermissionFunction.class);
        when(func1.getCode()).thenReturn("patient:view");
        when(func1.getEnabled()).thenReturn(true);

        var func2 = mock(PermissionFunction.class);
        when(func2.getCode()).thenReturn("patient:edit");
        when(func2.getEnabled()).thenReturn(true);

        var postFromRole = mock(Post.class);
        when(postFromRole.getCode()).thenReturn("ROLE_POST");
        when(postFromRole.getFunctions()).thenReturn(Set.of(func1));

        var role = mock(Role.class);
        when(role.getCode()).thenReturn("DOCTOR");
        when(role.getSort()).thenReturn(0);
        when(role.getEnabled()).thenReturn(true);
        when(role.getPosts()).thenReturn(Set.of(postFromRole));

        var postDirect = mock(Post.class);
        when(postDirect.getCode()).thenReturn("DIRECT_POST");
        when(postDirect.getFunctions()).thenReturn(Set.of(func2));

        var user = mock(User.class);
        when(user.getId()).thenReturn(4L);
        when(user.getUsername()).thenReturn("mergeduser");
        when(user.getNickname()).thenReturn("合并权限用户");
        when(user.getPhone()).thenReturn((String) null);
        when(user.getEmail()).thenReturn((String) null);
        when(user.getRoles()).thenReturn(Set.of(role));
        when(user.getPosts()).thenReturn(Set.of(postDirect));

        when(userRepository.findById(4L)).thenReturn(Optional.of(user));

        var result = userFacade.findById(4L);

        assertNotNull(result);
        assertTrue(result.permissions().contains("patient:view"));
        assertTrue(result.permissions().contains("patient:edit"));
    }

    @Test
    @DisplayName("findById Repository 抛出 DataAccessException 时传播异常")
    void findById_whenRepositoryThrowsDataAccessException_shouldPropagate() {
        when(userRepository.findById(any())).thenThrow(new DataAccessException("...") {});
        assertThrows(DataAccessException.class, () -> userFacade.findById(1L));
    }
}
