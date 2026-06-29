package com.aimedical.modules.commonmodule.auth.security;

import com.aimedical.common.exception.GlobalErrorCode;
import com.aimedical.modules.commonmodule.auth.blacklist.TokenBlacklist;
import com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProvider;
import com.aimedical.modules.commonmodule.permission.PermissionFunction;
import com.aimedical.modules.commonmodule.permission.Post;
import com.aimedical.modules.commonmodule.permission.Role;
import com.aimedical.modules.commonmodule.permission.User;
import com.aimedical.modules.commonmodule.permission.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final TokenBlacklist tokenBlacklist = mock(TokenBlacklist.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, tokenBlacklist, userRepository);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSkipWhenNoAuthHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldSkipWhenInvalidToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer xxx");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtTokenProvider.getTokenType()).thenReturn("Bearer");
        when(jwtTokenProvider.validateToken("xxx", "access")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldSkipWhenRefreshTokenType() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer xxx");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtTokenProvider.getTokenType()).thenReturn("Bearer");
        when(jwtTokenProvider.validateToken("xxx", "access")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(tokenBlacklist, never()).isBlacklisted(any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldSkipWhenTokenBlacklisted() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer xxx");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtTokenProvider.getTokenType()).thenReturn("Bearer");
        Claims claims = mock(Claims.class);
        when(claims.get("jti", String.class)).thenReturn("some-jti");
        when(jwtTokenProvider.validateToken("xxx", "access")).thenReturn(claims);
        when(tokenBlacklist.isBlacklisted("some-jti")).thenReturn(true);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldSkipWhenUserNotFound() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer xxx");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtTokenProvider.getTokenType()).thenReturn("Bearer");
        Claims claims = mock(Claims.class);
        when(claims.get("jti", String.class)).thenReturn(null);
        when(jwtTokenProvider.validateToken("xxx", "access")).thenReturn(claims);
        when(jwtTokenProvider.getUserIdFromClaims(claims)).thenReturn(1L);
        when(userRepository.findWithDetailsById(1L)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldThrowAccountDisabledWhenUserDisabled() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer xxx");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtTokenProvider.getTokenType()).thenReturn("Bearer");
        Claims claims = mock(Claims.class);
        when(claims.get("jti", String.class)).thenReturn(null);
        when(jwtTokenProvider.validateToken("xxx", "access")).thenReturn(claims);
        when(jwtTokenProvider.getUserIdFromClaims(claims)).thenReturn(1L);

        User user = mock(User.class);
        when(user.getEnabled()).thenReturn(false);
        when(userRepository.findWithDetailsById(1L)).thenReturn(Optional.of(user));

        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> filter.doFilterInternal(request, response, chain));
        assertEquals(GlobalErrorCode.ACCOUNT_DISABLED.getMessage(), ex.getMessage());
    }

    @Test
    void shouldAuthenticateSuccessfully() throws Exception {
        Long userId = 1L;
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        Claims claims = mock(Claims.class);
        when(claims.get("jti", String.class)).thenReturn(null);
        when(jwtTokenProvider.validateToken("valid-token", "access")).thenReturn(claims);
        when(jwtTokenProvider.getTokenType()).thenReturn("Bearer");
        when(jwtTokenProvider.getUserIdFromClaims(claims)).thenReturn(userId);

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getUsername()).thenReturn("testuser");
        when(user.getEnabled()).thenReturn(true);
        when(user.getPasswordChangeRequired()).thenReturn(false);
        when(user.getRoles()).thenReturn(Set.of());
        when(user.getPosts()).thenReturn(Set.of());
        when(userRepository.findWithDetailsById(userId)).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("testuser", auth.getPrincipal());
        assertTrue(auth.getAuthorities().isEmpty());
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldSetPasswordChangeRequiredAttribute() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer xxx");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtTokenProvider.getTokenType()).thenReturn("Bearer");
        Claims claims = mock(Claims.class);
        when(claims.get("jti", String.class)).thenReturn(null);
        when(jwtTokenProvider.validateToken("xxx", "access")).thenReturn(claims);
        when(jwtTokenProvider.getUserIdFromClaims(claims)).thenReturn(1L);

        User user = mock(User.class);
        when(user.getEnabled()).thenReturn(true);
        when(user.getPasswordChangeRequired()).thenReturn(true);
        when(user.getRoles()).thenReturn(Set.of());
        when(user.getPosts()).thenReturn(Set.of());
        when(userRepository.findWithDetailsById(1L)).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, chain);

        assertEquals(true, request.getAttribute("passwordChangeRequired"));
    }

    @Test
    void shouldPopulateAuthoritiesFromRolesAndFunctions() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer xxx");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtTokenProvider.getTokenType()).thenReturn("Bearer");
        Claims claims = mock(Claims.class);
        when(claims.get("jti", String.class)).thenReturn(null);
        when(jwtTokenProvider.validateToken("xxx", "access")).thenReturn(claims);
        when(jwtTokenProvider.getUserIdFromClaims(claims)).thenReturn(1L);

        Role roleA = mock(Role.class);
        when(roleA.getCode()).thenReturn("ADMIN");
        Role roleB = mock(Role.class);
        when(roleB.getCode()).thenReturn("DOCTOR");

        PermissionFunction func1 = mock(PermissionFunction.class);
        when(func1.getCode()).thenReturn("CREATE");
        PermissionFunction func2 = mock(PermissionFunction.class);
        when(func2.getCode()).thenReturn("READ");
        PermissionFunction func3 = mock(PermissionFunction.class);
        when(func3.getCode()).thenReturn("UPDATE");
        PermissionFunction func4 = mock(PermissionFunction.class);
        when(func4.getCode()).thenReturn("DELETE");

        Post postA = mock(Post.class);
        when(postA.getFunctions()).thenReturn(Set.of(func1, func2));
        Post postB = mock(Post.class);
        when(postB.getFunctions()).thenReturn(Set.of(func3, func4));

        User user = mock(User.class);
        when(user.getEnabled()).thenReturn(true);
        when(user.getPasswordChangeRequired()).thenReturn(false);
        when(user.getRoles()).thenReturn(Set.of(roleA, roleB));
        when(user.getPosts()).thenReturn(Set.of(postA, postB));
        when(userRepository.findWithDetailsById(1L)).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(6, auth.getAuthorities().size());
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_DOCTOR")));
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("FUNC_CREATE")));
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("FUNC_READ")));
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("FUNC_UPDATE")));
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("FUNC_DELETE")));
    }
}
