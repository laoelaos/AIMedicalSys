package com.aimedical.modules.commonmodule.auth.password;

import com.aimedical.common.exception.GlobalErrorCode;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicyImpl implements PasswordPolicy {
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;
    private static final String SPECIAL_CHARACTERS = "!@#$%^&*()_+-=[]{}|;:',.<>?/~";

    @Override
    public GlobalErrorCode validate(String password, String username) {
        if (password == null || password.length() < MIN_LENGTH) {
            return GlobalErrorCode.PASSWORD_TOO_SHORT;
        }
        if (password.length() > MAX_LENGTH) {
            return GlobalErrorCode.PASSWORD_TOO_LONG;
        }
        if (countCharTypes(password) < 3) {
            return GlobalErrorCode.PASSWORD_WEAK;
        }
        if (username != null && !username.isEmpty()
                && password.toLowerCase().contains(username.toLowerCase())) {
            return GlobalErrorCode.PASSWORD_CONTAINS_USERNAME;
        }
        return null;
    }

    private int countCharTypes(String password) {
        int types = 0;
        if (password.matches(".*[A-Z].*")) types++;
        if (password.matches(".*[a-z].*")) types++;
        if (password.matches(".*[0-9].*")) types++;
        if (containsSpecialChar(password)) types++;
        return types;
    }

    private boolean containsSpecialChar(String password) {
        for (char c : password.toCharArray()) {
            if (SPECIAL_CHARACTERS.indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }
}
