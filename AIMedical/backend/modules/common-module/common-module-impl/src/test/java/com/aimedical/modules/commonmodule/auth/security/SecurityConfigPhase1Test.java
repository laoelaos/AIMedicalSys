package com.aimedical.modules.commonmodule.auth.security;

import com.aimedical.common.util.MessageInterpolator;
import com.aimedical.modules.commonmodule.auth.blacklist.InMemoryTokenBlacklist;
import com.aimedical.modules.commonmodule.auth.blacklist.TokenBlacklist;
import com.aimedical.modules.commonmodule.auth.rateLimit.SlidingWindowCounter;
import com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProvider;
import com.aimedical.modules.commonmodule.permission.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.Filter;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigPhase1Test {

    private final SecurityConfigPhase1 config = new SecurityConfigPhase1();
    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    @Test
    void shouldCreateAllBeans() {
        SlidingWindowCounter counter = config.slidingWindowCounter();
        assertNotNull(counter);
        GlobalRateLimitFilter g = config.globalRateLimitFilter(counter);
        assertNotNull(g);
        JwtAuthenticationFilter j = config.jwtAuthenticationFilter(jwtTokenProvider, mock(TokenBlacklist.class), userRepository);
        assertNotNull(j);
        PasswordChangeCheckFilter p = config.passwordChangeCheckFilter();
        assertNotNull(p);
    }

    @Test
    void shouldReturnBCryptPasswordEncoder() {
        assertInstanceOf(BCryptPasswordEncoder.class, config.passwordEncoder());
    }

    @Test
    void shouldCreateJwtAuthenticationFilterWithDeps() {
        JwtAuthenticationFilter filter = config.jwtAuthenticationFilter(
                jwtTokenProvider, new InMemoryTokenBlacklist(), userRepository);
        assertNotNull(filter);
    }

    @Test
    void shouldRegisterFiltersInExpectedOrder() throws Exception {
        ObjectPostProcessor<Object> opp = new ObjectPostProcessor<>() {
            @Override
            public <O> O postProcess(O object) { return object; }
        };
        AuthenticationManagerBuilder amb = new AuthenticationManagerBuilder(opp);

        GlobalRateLimitFilter globalRateLimitFilter = config.globalRateLimitFilter(new SlidingWindowCounter());
        JwtAuthenticationFilter jwtAuthenticationFilter = config.jwtAuthenticationFilter(
                jwtTokenProvider, mock(TokenBlacklist.class), userRepository);
        PasswordChangeCheckFilter passwordChangeCheckFilter = config.passwordChangeCheckFilter();

        ApplicationContext appCtx = mock(ApplicationContext.class);
        when(appCtx.containsBean(any(String.class))).thenReturn(false);
        when(appCtx.containsBean("mvcHandlerMappingIntrospector")).thenReturn(true);
        when(appCtx.containsBeanDefinition(any(String.class))).thenReturn(false);
        when(appCtx.getBeanNamesForType(any(Class.class))).thenReturn(new String[0]);
        when(appCtx.getBean(anyString(), any(Class.class))).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Class<Object> type = (Class<Object>) inv.getArgument(1);
            return mock(type);
        });
        HashMap<Class<?>, Object> sharedMap = new HashMap<>();
        sharedMap.put(ApplicationContext.class, appCtx);
        HttpSecurity http = new HttpSecurity(opp, amb, sharedMap);

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(globalRateLimitFilter, JwtAuthenticationFilter.class);
        http.addFilterAfter(passwordChangeCheckFilter, JwtAuthenticationFilter.class);

        MessageInterpolator messageInterpolator = mock(MessageInterpolator.class);
        SecurityFilterChain chain = config.filterChain(http, globalRateLimitFilter, jwtAuthenticationFilter, passwordChangeCheckFilter, messageInterpolator);
        List<Filter> filters = chain.getFilters();

        List<Class<? extends Filter>> classes = filters.stream().map(Filter::getClass).toList();
        int idxGlobal = classes.indexOf(GlobalRateLimitFilter.class);
        int idxJwt   = classes.indexOf(JwtAuthenticationFilter.class);
        int idxPwd   = classes.indexOf(PasswordChangeCheckFilter.class);

        assertNotEquals(-1, idxGlobal, "GlobalRateLimitFilter must be registered");
        assertNotEquals(-1, idxJwt, "JwtAuthenticationFilter must be registered");
        assertNotEquals(-1, idxPwd, "PasswordChangeCheckFilter must be registered");
        assertTrue(idxGlobal < idxJwt, "GlobalRateLimitFilter must precede JwtAuthenticationFilter");
        assertTrue(idxJwt < idxPwd, "JwtAuthenticationFilter must precede PasswordChangeCheckFilter");
    }
}
