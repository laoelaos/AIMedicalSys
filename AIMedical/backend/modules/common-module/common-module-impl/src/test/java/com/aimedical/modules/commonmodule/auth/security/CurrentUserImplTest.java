package com.aimedical.modules.commonmodule.auth.security;

import com.aimedical.modules.commonmodule.api.UserType;
import com.aimedical.modules.commonmodule.permission.User;
import com.aimedical.modules.commonmodule.permission.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class CurrentUserImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CurrentUserImpl currentUser = new CurrentUserImpl(userRepository);
    private MockedStatic<SecurityContextHolder> securityContextHolderMock;
    private SecurityContext securityContext;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        securityContextHolderMock = mockStatic(SecurityContextHolder.class);
        securityContext = mock(SecurityContext.class);
        authentication = mock(Authentication.class);
        securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    @AfterEach
    void tearDown() {
        securityContextHolderMock.close();
    }

    @Test
    void getUserId_whenAuthenticated_shouldReturnUserId() {
        when(authentication.getPrincipal()).thenReturn(1L);

        Long result = currentUser.getUserId();

        assertEquals(1L, result);
    }

    @Test
    void getUserId_whenNoAuth_shouldReturnNull() {
        when(securityContext.getAuthentication()).thenReturn(null);

        Long result = currentUser.getUserId();

        assertNull(result);
    }

    @Test
    void getUsername_whenAuthenticated_shouldReturnUsername() {
        when(authentication.getPrincipal()).thenReturn(1L);
        User user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(user.getUsername()).thenReturn("doctor001");

        String result = currentUser.getUsername();

        assertEquals("doctor001", result);
    }

    @Test
    void getUserType_whenAuthenticated_shouldReturnUserType() {
        when(authentication.getPrincipal()).thenReturn(1L);
        User user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(user.getUserType()).thenReturn(UserType.DOCTOR);

        UserType result = currentUser.getUserType();

        assertEquals(UserType.DOCTOR, result);
    }

    @Test
    void getUsername_whenUserNotFound_shouldReturnNull() {
        when(authentication.getPrincipal()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        String result = currentUser.getUsername();

        assertNull(result);
    }

    @Test
    void getUserId_whenPrincipalIsNotLong_shouldReturnNull() {
        when(authentication.getPrincipal()).thenReturn("not a long");

        Long result = currentUser.getUserId();

        assertNull(result);
    }
}
