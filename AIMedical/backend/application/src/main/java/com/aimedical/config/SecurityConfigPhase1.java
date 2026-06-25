package com.aimedical.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 安全配置（Phase1版本）
 *
 * <p>配置Spring Security，允许登录接口匿名访问，其他接口需要JWT认证。
 * 启用方法级安全注解（@PreAuthorize）。
 *
 * <p>安全策略：
 * <ul>
 *   <li>H2 Console：仅当 spring.h2.console.enabled=true 时放行（开发环境），生产环境完全拒绝</li>
 *   <li>Actuator：仅 health 和 info 放行，其他端点需认证</li>
 * </ul>
 *
 * @author AIMedical Team
 * @version 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("phase1")
public class SecurityConfigPhase1 {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    public SecurityConfigPhase1(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                auth
                    .requestMatchers("/api/auth/login").permitAll()
                    .requestMatchers("/ping").permitAll()
                    .requestMatchers("/ping/**").permitAll()
                    .requestMatchers("/api/ping").permitAll()
                    // Actuator 仅 health 和 info 放行，其他端点需认证
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/actuator/info").permitAll()
                    .requestMatchers("/actuator/**").authenticated()
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .requestMatchers("/v3/api-docs/**").permitAll();

                // H2 Console：仅开发环境（spring.h2.console.enabled=true）放行；生产环境完全拒绝
                if (h2ConsoleEnabled) {
                    auth.requestMatchers("/h2-console/**").permitAll();
                } else {
                    auth.requestMatchers("/h2-console/**").denyAll();
                }

                auth.anyRequest().authenticated();
            })
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 密码编码器
     *
     * @return BCryptPasswordEncoder实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
