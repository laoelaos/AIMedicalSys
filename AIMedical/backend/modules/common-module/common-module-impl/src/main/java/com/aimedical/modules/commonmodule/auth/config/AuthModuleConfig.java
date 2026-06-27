package com.aimedical.modules.commonmodule.auth.config;

import com.aimedical.modules.commonmodule.auth.audit.LoggingSecurityAuditLogger;
import com.aimedical.modules.commonmodule.auth.audit.SecurityAuditLogger;
import com.aimedical.modules.commonmodule.auth.blacklist.InMemoryTokenBlacklist;
import com.aimedical.modules.commonmodule.auth.blacklist.TokenBlacklist;
import com.aimedical.modules.commonmodule.auth.login.LoginAttemptTracker;
import com.aimedical.modules.commonmodule.auth.rateLimit.InMemoryRateLimitGuard;
import com.aimedical.modules.commonmodule.auth.rateLimit.RateLimitGuard;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthModuleConfig {

    @Bean
    public RateLimitGuard rateLimitGuard() {
        return new InMemoryRateLimitGuard();
    }

    @Bean
    public TokenBlacklist tokenBlacklist() {
        return new InMemoryTokenBlacklist();
    }

    @Bean
    public LoginAttemptTracker loginAttemptTracker() {
        return new LoginAttemptTracker();
    }

    @Bean
    public SecurityAuditLogger securityAuditLogger() {
        return new LoggingSecurityAuditLogger();
    }
}
