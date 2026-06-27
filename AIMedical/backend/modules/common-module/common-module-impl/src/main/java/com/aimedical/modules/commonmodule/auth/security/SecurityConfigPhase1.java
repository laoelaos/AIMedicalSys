package com.aimedical.modules.commonmodule.auth.security;

import com.aimedical.common.util.MessageInterpolator;
import com.aimedical.modules.commonmodule.auth.blacklist.TokenBlacklist;
import com.aimedical.modules.commonmodule.auth.rateLimit.SlidingWindowCounter;
import com.aimedical.modules.commonmodule.auth.jwt.JwtTokenProvider;
import com.aimedical.modules.commonmodule.permission.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("phase1")
public class SecurityConfigPhase1 {

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SlidingWindowCounter slidingWindowCounter() {
        return new SlidingWindowCounter();
    }

    @Bean
    public GlobalRateLimitFilter globalRateLimitFilter(SlidingWindowCounter counter) {
        return new GlobalRateLimitFilter(counter);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider, TokenBlacklist tokenBlacklist, UserRepository userRepository) {
        return new JwtAuthenticationFilter(jwtTokenProvider, tokenBlacklist, userRepository);
    }

    @Bean
    public PasswordChangeCheckFilter passwordChangeCheckFilter() {
        return new PasswordChangeCheckFilter();
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            GlobalRateLimitFilter globalRateLimitFilter,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            PasswordChangeCheckFilter passwordChangeCheckFilter,
            MessageInterpolator messageInterpolator)
            throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(Customizer.withDefaults())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new RestAuthenticationEntryPoint(messageInterpolator))
                .accessDeniedHandler(new RestAccessDeniedHandler(messageInterpolator)))
            .authorizeHttpRequests(auth -> {
                auth
                    .requestMatchers("/api/auth/login").permitAll()
                    .requestMatchers("/api/auth/refresh").permitAll()
                    .requestMatchers("/api/auth/logout").authenticated()
                    .requestMatchers("/api/auth/**").authenticated()
                    .requestMatchers("/api/menu/**").authenticated()
                    .requestMatchers("/api/ping").permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/actuator/info").permitAll()
                    .requestMatchers("/actuator/**").denyAll()
                    .requestMatchers("/swagger-ui/**").denyAll()
                    .requestMatchers("/v3/api-docs/**").denyAll()
                    .requestMatchers("/error").permitAll();
                if (h2ConsoleEnabled) {
                    auth.requestMatchers("/h2-console/**").permitAll();
                } else {
                    auth.requestMatchers("/h2-console/**").denyAll();
                }
                auth.anyRequest().authenticated();
            })
            .headers(headers -> headers.frameOptions(fo -> fo.sameOrigin()))
            .addFilterBefore(globalRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(passwordChangeCheckFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
