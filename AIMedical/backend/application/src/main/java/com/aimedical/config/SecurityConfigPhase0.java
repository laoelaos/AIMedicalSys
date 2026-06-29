package com.aimedical.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Profile("phase0 & !phase1")
public class SecurityConfigPhase0 {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfigPhase0.class);

    @PostConstruct
    public void warnInsecureProfile() {
        log.warn("============================================================");
        log.warn("⚠ 安全警告：当前激活 phase0 Profile，所有接口 permitAll 全放行！");
        log.warn("⚠ 这意味着 @PreAuthorize 方法安全注解全部失效，任何匿名用户可访问全部接口。");
        log.warn("⚠ 仅限本地开发调试，生产环境严禁启用 phase0（应使用 phase1）。");
        log.warn("============================================================");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
