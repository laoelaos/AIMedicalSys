# 任务指令（v14）

## 动作
NEW

## 任务描述
在 `common-module-impl/auth/password/` 包下创建 PasswordPolicy 接口与 PasswordPolicyImpl 实现（封装密码复杂度校验规则）、PasswordChangeService 接口与 PasswordChangeServiceImpl 实现（封装密码变更策略管理），以及对应的单元测试。

**包路径**：`com.aimedical.modules.commonmodule.auth.password`

### 新建文件（相对于 `AIMedical/backend/`）

| 文件 | 路径 |
|------|------|
| PasswordPolicy.java | `modules/common-module/common-module-impl/src/main/java/.../auth/password/PasswordPolicy.java` |
| PasswordPolicyImpl.java | `modules/common-module/common-module-impl/src/main/java/.../auth/password/PasswordPolicyImpl.java` |
| PasswordChangeService.java | `modules/common-module/common-module-impl/src/main/java/.../auth/password/PasswordChangeService.java` |
| PasswordChangeServiceImpl.java | `modules/common-module/common-module-impl/src/main/java/.../auth/password/PasswordChangeServiceImpl.java` |
| PasswordPolicyImplTest.java | `modules/common-module/common-module-impl/src/test/java/.../auth/password/PasswordPolicyImplTest.java` |
| PasswordChangeServiceImplTest.java | `modules/common-module/common-module-impl/src/test/java/.../auth/password/PasswordChangeServiceImplTest.java` |

## 选择理由
- 紧接 R13 UserFacade 后的 Stage 3 核心抽象
- PasswordPolicy（H8）和 PasswordChangeService（OOD 3.4）均在 `password/` 包下，功能内聚
- 是 AuthServiceImpl.changePassword()/refresh()/login() 的前置依赖
- 无其他未完成任务的编译期依赖

## 任务上下文

### PasswordPolicy 接口
```java
package com.aimedical.modules.commonmodule.auth.password;

import com.aimedical.common.exception.GlobalErrorCode;

public interface PasswordPolicy {
    GlobalErrorCode validate(String password, String username);
}
```

- `password`：待校验的明文密码
- `username`：当前用户名，用于"密码不得包含用户名"规则
- 返回值：合规返回 `null`，不合规返回对应 `GlobalErrorCode` 枚举值

### PasswordPolicyImpl 实现
- `@Component`
- 校验规则（优先级顺序）：
  1. 最小长度 8 → `GlobalErrorCode.PASSWORD_TOO_SHORT`
  2. 最大长度 64 → `GlobalErrorCode.PASSWORD_TOO_LONG`
  3. 字符种类：至少包含大写字母、小写字母、数字、特殊字符中的 **3 种** → `GlobalErrorCode.PASSWORD_WEAK`
  4. 密码不得包含用户名（大小写不敏感） → `GlobalErrorCode.PASSWORD_CONTAINS_USERNAME`
- 特殊字符定义：`!@#$%^&*()_+-=[]{}|;:',.<>?/~`（可根据实际需要扩展）
- 方法实现：按顺序逐条检查，命中任意规则立即返回对应 ErrorCode

### PasswordChangeService 接口
```java
package com.aimedical.modules.commonmodule.auth.password;

public interface PasswordChangeService {
    boolean isChangeRequired(Long userId);
    void markChangeRequired(Long userId);
    void clearChangeRequired(Long userId);
}
```

### PasswordChangeServiceImpl 实现
- `@Component`，注入 `UserRepository`
- `isChangeRequired(Long userId)`：通过 `userRepository.findById(userId)` 加载 `User`，返回 `user.getPasswordChangeRequired()`（false 当用户不存在时）
- `markChangeRequired(Long userId)`：加载 User，设 `passwordChangeRequired = true`，`userRepository.save(user)`
- `clearChangeRequired(Long userId)`：加载 User，设 `passwordChangeRequired = false`，`userRepository.save(user)`
- 用户不存在时，`isChangeRequired` 返回 false；`markChangeRequired`/`clearChangeRequired` 静默跳过

### 已有类型依赖
| 类型 | 包 | 说明 |
|------|-----|------|
| `GlobalErrorCode` | `com.aimedical.common.exception` | PASSWORD_TOO_SHORT / PASSWORD_TOO_LONG / PASSWORD_WEAK / PASSWORD_CONTAINS_USERNAME 已在 R2 中扩展 |
| `UserRepository` | `com.aimedical.modules.commonmodule.permission` | 用于 PasswordChangeServiceImpl 查询/更新 User |
| `User` | `com.aimedical.modules.commonmodule.permission` | 实体，含 passwordChangeRequired 字段（R1 新增） |
| `PasswordChangeRequiredException` | `com.aimedical.modules.commonmodule.auth.exception` | R8 已创建 |

## 单元测试设计

### PasswordPolicyImplTest
**形态**：JUnit 5 纯单元测试（无 Spring 上下文），直接 new PasswordPolicyImpl()

**测试方法清单**（9 用例）：

| # | 方法名 | 输入 | 预期结果 |
|---|--------|------|---------|
| 1 | `validate_whenPasswordTooShort_shouldReturnTooShort` | password="Ab1!", username="test" | PASSWORD_TOO_SHORT |
| 2 | `validate_whenPasswordTooLong_shouldReturnTooLong` | password="A" + "a1!" repeated 64 times, username="test" | PASSWORD_TOO_LONG |
| 3 | `validate_whenOnlyOneCharType_shouldReturnWeak` | password="aaaaaaaa", username="test" | PASSWORD_WEAK |
| 4 | `validate_whenOnlyTwoCharTypes_shouldReturnWeak` | password="aaaaaaA1", username="test" | PASSWORD_WEAK |
| 5 | `validate_whenContainsUsername_shouldReturnContainsUsername` | password="Abcd1234!test", username="Test" | PASSWORD_CONTAINS_USERNAME |
| 6 | `validate_whenMeetsAllRequirements_shouldReturnNull` | password="Abcd1234!", username="test" | null |
| 7 | `validate_whenThreeCharTypesWithSpecial_shouldReturnNull` | password="abcABC!@#", username="test" | null |
| 8 | `validate_whenThreeCharTypesWithDigit_shouldReturnNull` | password="abcABC12345", username="test" | null |
| 9 | `validate_whenThreeCharTypesWithUpper_shouldReturnNull` | password="abc123!@#", username="test" | null |

- 特殊字符集合至少包含：`!@#$%^&*()_+-=[]{}|;:',.<>?/~`
- 字符种类计算：大写字母 `[A-Z]`、小写字母 `[a-z]`、数字 `[0-9]`、特殊字符（集合中任意一个）
- 用户名包含检查：`password.toLowerCase().contains(username.toLowerCase())`

### PasswordChangeServiceImplTest
**形态**：JUnit 5 + Mockito，mock `UserRepository`

**测试方法清单**（6 用例）：

| # | 方法名 | Arrange | 验证 |
|---|--------|---------|------|
| 1 | `isChangeRequired_whenUserExistsAndFlagTrue_shouldReturnTrue` | userRepository.findById(1L) → Optional.of(user), user.getPasswordChangeRequired() → true | assertTrue |
| 2 | `isChangeRequired_whenUserExistsAndFlagFalse_shouldReturnFalse` | userRepository.findById(1L) → Optional.of(user), user.getPasswordChangeRequired() → false | assertFalse |
| 3 | `isChangeRequired_whenUserNotFound_shouldReturnFalse` | userRepository.findById(999L) → Optional.empty() | assertFalse |
| 4 | `markChangeRequired_shouldSetFlagAndSave` | userRepository.findById(1L) → Optional.of(user) | verify(user).setPasswordChangeRequired(true); verify(userRepository).save(user) |
| 5 | `clearChangeRequired_shouldClearFlagAndSave` | userRepository.findById(1L) → Optional.of(user) | verify(user).setPasswordChangeRequired(false); verify(userRepository).save(user) |
| 6 | `markChangeRequired_whenUserNotFound_shouldSkipSilently` | userRepository.findById(999L) → Optional.empty() | verify(userRepository, never()).save(any()); no exception thrown |

## 测试命令
```bash
mvn test -pl modules/common-module/common-module-impl -am -Dtest="PasswordPolicyImplTest,PasswordChangeServiceImplTest" -Dsurefire.failIfNoSpecifiedTests=false
```
