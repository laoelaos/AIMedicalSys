package com.aimedical.modules.commonmodule.auth;

import com.aimedical.modules.commonmodule.auth.converter.UserConverter;
import com.aimedical.modules.commonmodule.permission.User;
import com.aimedical.modules.commonmodule.permission.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class UserFacadeImpl implements UserFacade {
    private final UserRepository userRepository;
    private final UserConverter userConverter;

    public UserFacadeImpl(UserRepository userRepository, UserConverter userConverter) {
        this.userRepository = userRepository;
        this.userConverter = userConverter;
    }

    @Override
    public UserInfoResponse findById(Long userId) {
        return userRepository.findById(userId)
                .map(this::toUserInfoResponse)
                .orElse(null);
    }

    @Override
    public UserInfoResponse findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::toUserInfoResponse)
                .orElse(null);
    }

    @Override
    public boolean existsById(Long userId) {
        return userRepository.existsById(userId);
    }

    private UserInfoResponse toUserInfoResponse(User user) {
        return userConverter.toUserInfoResponse(user);
    }
}
