package com.aimedical.modules.commonmodule.auth.password;

import com.aimedical.modules.commonmodule.permission.User;
import com.aimedical.modules.commonmodule.permission.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("PasswordChangeServiceImpl")
class PasswordChangeServiceImplTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordChangeServiceImpl service = new PasswordChangeServiceImpl(userRepository);

    @Test
    @DisplayName("isChangeRequired 用户存在且标记为 true 时应返回 true")
    void isChangeRequired_whenUserExistsAndFlagTrue_shouldReturnTrue() {
        var user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(user.getPasswordChangeRequired()).thenReturn(true);

        var result = service.isChangeRequired(1L);

        assertTrue(result);
    }

    @Test
    @DisplayName("isChangeRequired 用户存在且标记为 false 时应返回 false")
    void isChangeRequired_whenUserExistsAndFlagFalse_shouldReturnFalse() {
        var user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(user.getPasswordChangeRequired()).thenReturn(false);

        var result = service.isChangeRequired(1L);

        assertFalse(result);
    }

    @Test
    @DisplayName("isChangeRequired 用户不存在时应返回 false")
    void isChangeRequired_whenUserNotFound_shouldReturnFalse() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        var result = service.isChangeRequired(999L);

        assertFalse(result);
    }

    @Test
    @DisplayName("markChangeRequired 应设置标记并保存")
    void markChangeRequired_shouldSetFlagAndSave() {
        var user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        service.markChangeRequired(1L);

        verify(user).setPasswordChangeRequired(true);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("clearChangeRequired 应清除标记并保存")
    void clearChangeRequired_shouldClearFlagAndSave() {
        var user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        service.clearChangeRequired(1L);

        verify(user).setPasswordChangeRequired(false);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("markChangeRequired 用户不存在时应静默跳过")
    void markChangeRequired_whenUserNotFound_shouldSkipSilently() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.markChangeRequired(999L));
        verify(userRepository, never()).save(any());
    }
}
