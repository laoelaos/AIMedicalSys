package com.aimedical.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigPhase0Test {

    @Test
    void shouldBuildSecurityFilterChain() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class);
        when(http.csrf(any(Customizer.class))).thenReturn(http);
        when(http.authorizeHttpRequests(any(Customizer.class))).thenReturn(http);
        DefaultSecurityFilterChain expected = mock(DefaultSecurityFilterChain.class);
        when(http.build()).thenReturn(expected);

        SecurityConfigPhase0 config = new SecurityConfigPhase0();
        SecurityFilterChain result = config.filterChain(http);

        assertSame(expected, result);
    }
}