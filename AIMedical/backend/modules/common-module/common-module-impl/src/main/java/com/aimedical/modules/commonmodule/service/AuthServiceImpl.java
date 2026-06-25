package com.aimedical.modules.commonmodule.service;

import com.aimedical.common.exception.BusinessException;
import com.aimedical.modules.commonmodule.api.AuthService;
import com.aimedical.modules.commonmodule.api.PatientErrorCode;
import com.aimedical.modules.commonmodule.api.TokenProvider;
import com.aimedical.modules.commonmodule.api.UserType;
import com.aimedical.modules.commonmodule.api.dto.CurrentUserResponse;
import com.aimedical.modules.commonmodule.api.dto.LoginRequest;
import com.aimedical.modules.commonmodule.api.dto.RegisterRequest;
import com.aimedical.modules.commonmodule.api.dto.TokenResponse;
import com.aimedical.modules.commonmodule.permission.Role;
import com.aimedical.modules.commonmodule.permission.RoleRepository;
import com.aimedical.modules.commonmodule.permission.User;
import com.aimedical.modules.commonmodule.permission.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    public AuthServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           TokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getPhone())) {
            throw new BusinessException(PatientErrorCode.PATIENT_MOBILE_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getPhone());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getName());
        user.setGender(request.getGender());
        user.setAge(request.getAge());
        user.setEnabled(true);
        user.setUserType(UserType.PATIENT);

        Role patientRole = roleRepository.findByCode("PATIENT")
                .orElseGet(this::createPatientRole);
        user.setRoles(Collections.singleton(patientRole));

        user = userRepository.save(user);
        log.info("Patient registered: userId={}, phone={}", user.getId(), user.getPhone());

        return tokenProvider.generateTokens(user.getId(), user.getUsername());
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getPhone())
                .orElseThrow(() -> new BusinessException(PatientErrorCode.PATIENT_LOGIN_FAILED));

        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(PatientErrorCode.PATIENT_LOGIN_FAILED);
        }

        if (user.getEnabled() == null || !user.getEnabled()) {
            throw new BusinessException(PatientErrorCode.PATIENT_ACCOUNT_DISABLED);
        }

        // Set Spring Security context
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user.getUsername(), null,
                        user.getRoles() != null
                                ? user.getRoles().stream()
                                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getCode()))
                                    .collect(Collectors.toList())
                                : Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return tokenProvider.generateTokens(user.getId(), user.getUsername());
    }

    @Override
    public TokenResponse refresh(String refreshToken) {
        if (!tokenProvider.validateRefreshToken(refreshToken)) {
            throw new BusinessException(PatientErrorCode.PATIENT_TOKEN_INVALID);
        }
        String username = tokenProvider.getUsernameFromToken(refreshToken);
        if (username == null) {
            throw new BusinessException(PatientErrorCode.PATIENT_TOKEN_INVALID);
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(PatientErrorCode.PATIENT_LOGIN_FAILED));

        // Invalidate old refresh token and its family (rotation)
        tokenProvider.invalidateRefreshTokenFamily(refreshToken);

        // Issue new token pair
        TokenResponse tokens = tokenProvider.generateTokens(user.getId(), user.getUsername());
        log.debug("Refresh token rotated for user: {}", username);
        return tokens;
    }

    @Override
    public void logout(String accessToken) {
        tokenProvider.invalidateToken(accessToken);
        SecurityContextHolder.clearContext();
    }

    @Override
    public CurrentUserResponse getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(PatientErrorCode.PATIENT_LOGIN_FAILED);
        }
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(PatientErrorCode.PATIENT_LOGIN_FAILED));

        CurrentUserResponse resp = new CurrentUserResponse();
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setPhone(user.getPhone());
        resp.setGender(user.getGender());
        resp.setAge(user.getAge());
        resp.setUserType(user.getUserType() != null ? user.getUserType().getCode() : null);
        resp.setRoles(user.getRoles() != null
                ? user.getRoles().stream().map(Role::getCode).collect(Collectors.toList())
                : Collections.emptyList());
        return resp;
    }

    private Role createPatientRole() {
        Role role = new Role();
        role.setCode("PATIENT");
        role.setName("患者");
        role.setDescription("患者角色");
        role.setEnabled(true);
        return roleRepository.save(role);
    }
}
