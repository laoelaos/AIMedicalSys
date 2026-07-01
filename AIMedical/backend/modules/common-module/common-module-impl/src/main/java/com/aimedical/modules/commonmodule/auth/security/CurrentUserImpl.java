package com.aimedical.modules.commonmodule.auth.security;

import com.aimedical.modules.commonmodule.auth.CurrentUser;
import com.aimedical.modules.commonmodule.api.UserType;
import com.aimedical.modules.commonmodule.permission.User;
import com.aimedical.modules.commonmodule.permission.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserImpl implements CurrentUser {
    private final UserRepository userRepository;

    public CurrentUserImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Long uid) {
            return uid;
        }
        if (principal instanceof Integer uid) {
            return uid.longValue();
        }
        if (principal instanceof String username) {
            return userRepository.findByUsername(username)
                    .map(User::getId)
                    .orElse(null);
        }
        return null;
    }

    @Override
    public String getUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof String username) {
            return username;
        }
        Long userId = getUserId();
        if (userId == null) return null;
        return userRepository.findById(userId)
                .map(User::getUsername)
                .orElse(null);
    }

    @Override
    public UserType getUserType() {
        Long userId = getUserId();
        if (userId == null) return null;
        return userRepository.findById(userId)
                .map(User::getUserType)
                .orElse(null);
    }
}
