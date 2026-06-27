package com.aimedical.modules.commonmodule.auth.password;

import com.aimedical.modules.commonmodule.permission.User;
import com.aimedical.modules.commonmodule.permission.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class PasswordChangeServiceImpl implements PasswordChangeService {
    private final UserRepository userRepository;

    public PasswordChangeServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean isChangeRequired(Long userId) {
        return userRepository.findById(userId)
                .map(User::getPasswordChangeRequired)
                .orElse(false);
    }

    @Override
    public void markChangeRequired(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setPasswordChangeRequired(true);
            userRepository.save(user);
        });
    }

    @Override
    public void clearChangeRequired(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setPasswordChangeRequired(false);
            userRepository.save(user);
        });
    }
}
