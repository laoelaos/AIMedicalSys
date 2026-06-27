package com.aimedical.modules.commonmodule.auth.security;

import com.aimedical.modules.commonmodule.auth.blacklist.TokenBlacklist;
import com.aimedical.modules.commonmodule.auth.config.AuthModuleConfig;
import com.aimedical.modules.commonmodule.auth.rateLimit.SlidingWindowCounter;
import com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProvider;
import com.aimedical.modules.commonmodule.permission.UserRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class SecurityConfigPhase1CoexistenceTest {

    private final AuthModuleConfig authModuleConfig = new AuthModuleConfig();
    private final SecurityConfigPhase1 securityConfigPhase1 = new SecurityConfigPhase1();

    @Test
    void authModuleConfigTokenBlacklist_shouldBeUsableBySecurityConfigPhase1Filter() {
        TokenBlacklist tokenBlacklist = authModuleConfig.tokenBlacklist();
        assertNotNull(tokenBlacklist);

        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        JwtAuthenticationFilter filter = securityConfigPhase1.jwtAuthenticationFilter(
                jwtTokenProvider, tokenBlacklist, userRepository);
        assertNotNull(filter);
    }

    @Test
    void bothConfigs_shouldCoexistWithoutConflict() {
        SlidingWindowCounter counter = securityConfigPhase1.slidingWindowCounter();
        assertNotNull(counter);
        assertNotNull(securityConfigPhase1.globalRateLimitFilter(counter));
        assertNotNull(securityConfigPhase1.passwordEncoder());
        assertNotNull(securityConfigPhase1.passwordChangeCheckFilter());

        assertNotNull(authModuleConfig.rateLimitGuard());
        assertNotNull(authModuleConfig.tokenBlacklist());
        assertNotNull(authModuleConfig.loginAttemptTracker());
    }
}
