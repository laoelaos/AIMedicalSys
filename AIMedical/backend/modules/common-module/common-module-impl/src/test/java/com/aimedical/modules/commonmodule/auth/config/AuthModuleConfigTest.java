package com.aimedical.modules.commonmodule.auth.config;

import com.aimedical.modules.commonmodule.auth.audit.SecurityAuditLogger;
import com.aimedical.modules.commonmodule.auth.blacklist.TokenBlacklist;
import com.aimedical.modules.commonmodule.auth.login.LoginAttemptTracker;
import com.aimedical.modules.commonmodule.auth.rateLimit.RateLimitGuard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuthModuleConfigTest {

    private final AuthModuleConfig config = new AuthModuleConfig();

    @Test
    void rateLimitGuard_shouldReturnNonNullInstance() {
        RateLimitGuard guard = config.rateLimitGuard();
        assertNotNull(guard);
    }

    @Test
    void tokenBlacklist_shouldReturnNonNullInstance() {
        TokenBlacklist blacklist = config.tokenBlacklist();
        assertNotNull(blacklist);
    }

    @Test
    void loginAttemptTracker_shouldReturnNonNullInstance() {
        LoginAttemptTracker tracker = config.loginAttemptTracker();
        assertNotNull(tracker);
    }

    @Test
    void securityAuditLogger_shouldReturnNonNullInstance() {
        SecurityAuditLogger logger = config.securityAuditLogger();
        assertNotNull(logger);
    }
}
