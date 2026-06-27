package com.aimedical.modules.commonmodule.auth.password;

import com.aimedical.common.exception.GlobalErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PasswordPolicyImpl")
class PasswordPolicyImplTest {
    private final PasswordPolicyImpl policy = new PasswordPolicyImpl();

    @Test
    @DisplayName("密码长度小于8时应返回 PASSWORD_TOO_SHORT")
    void validate_whenPasswordTooShort_shouldReturnTooShort() {
        assertEquals(GlobalErrorCode.PASSWORD_TOO_SHORT, policy.validate("Ab1!", "test"));
    }

    @Test
    @DisplayName("密码长度超过64时应返回 PASSWORD_TOO_LONG")
    void validate_whenPasswordTooLong_shouldReturnTooLong() {
        String password = "A" + "a1!".repeat(64);
        assertEquals(GlobalErrorCode.PASSWORD_TOO_LONG, policy.validate(password, "test"));
    }

    @Test
    @DisplayName("仅包含1种字符类型时应返回 PASSWORD_WEAK")
    void validate_whenOnlyOneCharType_shouldReturnWeak() {
        assertEquals(GlobalErrorCode.PASSWORD_WEAK, policy.validate("aaaaaaaa", "test"));
    }

    @Test
    @DisplayName("仅包含2种字符类型时应返回 PASSWORD_WEAK")
    void validate_whenOnlyTwoCharTypes_shouldReturnWeak() {
        assertEquals(GlobalErrorCode.PASSWORD_WEAK, policy.validate("aaaaaaAA", "test"));
    }

    @Test
    @DisplayName("密码包含用户名时应返回 PASSWORD_CONTAINS_USERNAME")
    void validate_whenContainsUsername_shouldReturnContainsUsername() {
        assertEquals(GlobalErrorCode.PASSWORD_CONTAINS_USERNAME, policy.validate("Abcd1234!test", "Test"));
    }

    @Test
    @DisplayName("密码满足所有要求时应返回 null")
    void validate_whenMeetsAllRequirements_shouldReturnNull() {
        assertNull(policy.validate("Abcd1234!", "test"));
    }

    @Test
    @DisplayName("3种字符类型含特殊字符时应返回 null")
    void validate_whenThreeCharTypesWithSpecial_shouldReturnNull() {
        assertNull(policy.validate("abcABC!@#", "test"));
    }

    @Test
    @DisplayName("3种字符类型含数字时应返回 null")
    void validate_whenThreeCharTypesWithDigit_shouldReturnNull() {
        assertNull(policy.validate("abcABC12345", "test"));
    }

    @Test
    @DisplayName("3种字符类型含大写时应返回 null")
    void validate_whenThreeCharTypesWithUpper_shouldReturnNull() {
        assertNull(policy.validate("abc123!@#", "test"));
    }

    @Test
    @DisplayName("包含4种字符类型时应返回 null")
    void validate_whenAllFourCharTypes_shouldReturnNull() {
        assertNull(policy.validate("Abc1!xyz", "test"));
    }
}
