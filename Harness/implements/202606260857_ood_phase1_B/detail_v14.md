# 详细设计（v14）

## 概述

在 `common-module-impl` 的 `auth.password` 包下新建密码领域的两组抽象：
1. **PasswordPolicy 接口 + PasswordPolicyImpl 实现**：封装密码复杂度校验规则，供 `AuthServiceImpl.changePassword()` 等调用方使用
2. **PasswordChangeService 接口 + PasswordChangeServiceImpl 实现**：封装密码变更标记管理，供 `PasswordChangeCheckFilter`、`AuthServiceImpl` 等调用方使用

## 文件规划

| 文件路径（相对 `AIMedical/backend/`） | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-impl/src/main/java/.../auth/password/PasswordPolicy.java` | 新建 | 密码校验策略接口 |
| `modules/common-module/common-module-impl/src/main/java/.../auth/password/PasswordPolicyImpl.java` | 新建 | `@Component` 实现，封装 4 条规则校验 |
| `modules/common-module/common-module-impl/src/main/java/.../auth/password/PasswordChangeService.java` | 新建 | 密码变更标记管理接口 |
| `modules/common-module/common-module-impl/src/main/java/.../auth/password/PasswordChangeServiceImpl.java` | 新建 | `@Component` 实现，注入 `UserRepository` |
| `modules/common-module/common-module-impl/src/test/java/.../auth/password/PasswordPolicyImplTest.java` | 新建 | 9 个 JUnit 5 纯单元测试用例 |
| `modules/common-module/common-module-impl/src/test/java/.../auth/password/PasswordChangeServiceImplTest.java` | 新建 | 6 个 JUnit 5 + Mockito 单元测试用例 |

## 类型定义

### PasswordPolicy

**形态**：`public interface`
**包路径**：`com.aimedical.modules.commonmodule.auth.password`
**职责**：密码复杂度校验策略契约。校验密码是否满足系统定义的复杂度规则，不合规时返回对应的错误码。

```java
package com.aimedical.modules.commonmodule.auth.password;

import com.aimedical.common.exception.GlobalErrorCode;

public interface PasswordPolicy {
    GlobalErrorCode validate(String password, String username);
}
```

**公开接口**：

| 方法签名 | 返回 | 说明 |
|---------|------|------|
| `GlobalErrorCode validate(String password, String username)` | `GlobalErrorCode` | 密码合规返回 null，不合规返回对应枚举值 |

**构造方式**：无（接口），由 `PasswordPolicyImpl` 通过 `@Component` 提供实例。

**类型关系**：无继承/实现关系。

---

### PasswordPolicyImpl

**形态**：`@Component` class，实现 `PasswordPolicy`
**包路径**：`com.aimedical.modules.commonmodule.auth.password`
**职责**：`PasswordPolicy` 接口的 `@Component` 实现。按固定优先级顺序执行 4 条规则校验，命中即返回。

```java
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
        // 规则 1: 最小长度
        if (password == null || password.length() < MIN_LENGTH) {
            return GlobalErrorCode.PASSWORD_TOO_SHORT;
        }
        // 规则 2: 最大长度
        if (password.length() > MAX_LENGTH) {
            return GlobalErrorCode.PASSWORD_TOO_LONG;
        }
        // 规则 3: 字符种类 >= 3
        if (countCharTypes(password) < 3) {
            return GlobalErrorCode.PASSWORD_WEAK;
        }
        // 规则 4: 不得包含用户名（大小写不敏感）
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
```

**公开接口**：

| 方法签名 | 返回 | 说明 |
|---------|------|------|
| `GlobalErrorCode validate(String password, String username)` | `GlobalErrorCode` | 按顺序检查 4 条规则；全部通过返回 null |

**构造方式**：无参构造器，由 Spring `@Component` 扫描自动创建单例。

**类型关系**：实现 `PasswordPolicy`。

**校验规则优先级**：
1. 密码为 null 或长度 < 8 → `PASSWORD_TOO_SHORT`
2. 长度 > 64 → `PASSWORD_TOO_LONG`
3. 字符种类数 < 3（大写字母、小写字母、数字、特殊字符） → `PASSWORD_WEAK`
4. 密码（小写）包含用户名（小写） → `PASSWORD_CONTAINS_USERNAME`
5. 全部通过 → `null`

**字符种类定义**：
- 大写字母：`[A-Z]`
- 小写字母：`[a-z]`
- 数字：`[0-9]`
- 特殊字符：`!@#$%^&*()_+-=[]{}|;:',.<>?/~` 中的任意单个字符

**特殊字符检测**：遍历密码字符串逐字符检查是否在 `SPECIAL_CHARACTERS` 常量中。

---

### PasswordChangeService

**形态**：`public interface`
**包路径**：`com.aimedical.modules.commonmodule.auth.password`
**职责**：密码变更标记管理契约。提供查询、标记、清除"需修改密码"状态的能力。

```java
package com.aimedical.modules.commonmodule.auth.password;

public interface PasswordChangeService {
    boolean isChangeRequired(Long userId);
    void markChangeRequired(Long userId);
    void clearChangeRequired(Long userId);
}
```

**公开接口**：

| 方法签名 | 返回 | 说明 |
|---------|------|------|
| `boolean isChangeRequired(Long userId)` | `boolean` | 用户存在且 passwordChangeRequired 为 true 返回 true，否则 false |
| `void markChangeRequired(Long userId)` | `void` | 标记用户需修改密码（设为 true）并保存 |
| `void clearChangeRequired(Long userId)` | `void` | 清除密码变更标记（设为 false）并保存 |

**构造方式**：无（接口），由 `PasswordChangeServiceImpl` 通过 `@Component` 提供实例。

**类型关系**：无继承/实现关系。

---

### PasswordChangeServiceImpl

**形态**：`@Component` class，实现 `PasswordChangeService`
**包路径**：`com.aimedical.modules.commonmodule.auth.password`
**职责**：`PasswordChangeService` 接口的 `@Component` 实现。通过 `UserRepository` 查询/更新 `User.passwordChangeRequired` 字段。

```java
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
```

**公开接口**：

| 方法签名 | 返回 | 说明 |
|---------|------|------|
| `boolean isChangeRequired(Long userId)` | `boolean` | 委托 `userRepository.findById()`，存在则返回 `user.getPasswordChangeRequired()`，不存在返回 false |
| `void markChangeRequired(Long userId)` | `void` | 委托 `userRepository.findById()`，存在则 set true 并 save，不存在静默跳过 |
| `void clearChangeRequired(Long userId)` | `void` | 委托 `userRepository.findById()`，存在则 set false 并 save，不存在静默跳过 |

**构造方式**：通过 `@Component` + 构造器注入 `UserRepository`，由 Spring 扫描后自动创建单例。

**类型关系**：实现 `PasswordChangeService`。

## 错误处理

| 类型 | 策略 |
|------|------|
| `PasswordPolicy.validate()` | 返回 `GlobalErrorCode` 枚举值表示错误，null 表示成功。不抛异常 |
| `PasswordChangeService.isChangeRequired()` | 用户不存在时返回 false，不抛异常 |
| `PasswordChangeService.markChangeRequired()` | 用户不存在时静默跳过，不抛异常 |
| `PasswordChangeService.clearChangeRequired()` | 用户不存在时静默跳过，不抛异常 |

所有组件均无需自定义异常类型。已有的 `PasswordChangeRequiredException` 由 `PasswordChangeCheckFilter` 使用，与此设计无直接关系。

## 行为契约

### PasswordPolicy.validate()

| 项 | 值 |
|------|------|
| 前置条件 | 无（password 为 null 时直接返回 `PASSWORD_TOO_SHORT`） |
| 后置条件 | 不修改任何状态，纯函数 |
| 规则顺序 | 按优先级依次检查，命中即返回，不短路后续规则 |

### PasswordChangeService 方法

| 方法 | 前置条件 | 后置条件 |
|------|---------|---------|
| `isChangeRequired(Long)` | userId 可为 null（此时返回 false） | 只读查询，不修改数据库 |
| `markChangeRequired(Long)` | userId 可为 null（此时静默跳过） | 用户存在时 `passwordChangeRequired` 变为 true 并持久化 |
| `clearChangeRequired(Long)` | userId 可为 null（此时静默跳过） | 用户存在时 `passwordChangeRequired` 变为 false 并持久化 |

### 调用顺序

三个方法相互独立，可任意顺序调用。每次调用均单独触发 `userRepository.findById()` 查询。

## 依赖关系

### 本任务新建类型

| 类型 | 所在模块 | 说明 |
|------|---------|------|
| `PasswordPolicy` | `common-module-impl`（`com.aimedical.modules.commonmodule.auth.password`） | 接口，新建 |
| `PasswordPolicyImpl` | `common-module-impl`（`com.aimedical.modules.commonmodule.auth.password`） | `@Component` 实现，新建 |
| `PasswordChangeService` | `common-module-impl`（`com.aimedical.modules.commonmodule.auth.password`） | 接口，新建 |
| `PasswordChangeServiceImpl` | `common-module-impl`（`com.aimedical.modules.commonmodule.auth.password`） | `@Component` 实现，新建 |
| `PasswordPolicyImplTest` | `common-module-impl` test（`com.aimedical.modules.commonmodule.auth.password`） | 9 用例，JUnit 5 纯单元测试 |
| `PasswordChangeServiceImplTest` | `common-module-impl` test（`com.aimedical.modules.commonmodule.auth.password`） | 6 用例，JUnit 5 + Mockito |

### 依赖的已有类型

| 类型 | 所在包 | 说明 |
|------|--------|------|
| `GlobalErrorCode` | `com.aimedical.common.exception` | `PasswordPolicyImpl.validate()` 返回的枚举值。PASSWORD_TOO_SHORT / PASSWORD_TOO_LONG / PASSWORD_WEAK / PASSWORD_CONTAINS_USERNAME 已在 GlobalErrorCode 中定义 |
| `UserRepository` | `com.aimedical.modules.commonmodule.permission` | Spring Data JPA Repository。`PasswordChangeServiceImpl` 注入用于按 ID 查询/更新 User |
| `User` | `com.aimedical.modules.commonmodule.permission` | JPA 实体。`PasswordChangeServiceImpl` 从中读取/写入 `passwordChangeRequired` 字段 |

### 框架依赖

| 类型 | 说明 |
|------|------|
| `org.springframework.stereotype.Component` | `PasswordPolicyImpl`、`PasswordChangeServiceImpl` 的 Spring 组件注解 |
| `org.junit.jupiter.api.Test` | JUnit 5 测试注解 |
| `org.mockito.Mockito` | Mockito mock 框架（`PasswordChangeServiceImplTest`） |

### 暴露给后续任务的公开接口

- `PasswordPolicy` 接口 — `AuthServiceImpl.changePassword()` 可注入并调用 `validate()` 校验新密码
- `PasswordChangeService` 接口 — `AuthServiceImpl.changePassword()` 可调用 `clearChangeRequired()`，`login()` 可调用 `isChangeRequired()` 判断是否需要强制改密；`PasswordChangeCheckFilter` 可注入使用

## 单元测试设计

### PasswordPolicyImplTest

**形态**：class（JUnit 5），纯单元测试（无 Spring 上下文），直接 `new PasswordPolicyImpl()`。

**包路径**：`com.aimedical.modules.commonmodule.auth.password`

**测试夹具**：
```java
class PasswordPolicyImplTest {
    private final PasswordPolicyImpl policy = new PasswordPolicyImpl();
}
```

**测试方法清单**（9 用例）：

| # | 测试方法 | Arrange | Assert |
|---|---------|---------|--------|
| 1 | `validate_whenPasswordTooShort_shouldReturnTooShort` | password = `"Ab1!"`, username = `"test"` | `PASSWORD_TOO_SHORT` |
| 2 | `validate_whenPasswordTooLong_shouldReturnTooLong` | password = `"A" + "a1!"`.repeat(64), username = `"test"` (长度 > 64) | `PASSWORD_TOO_LONG` |
| 3 | `validate_whenOnlyOneCharType_shouldReturnWeak` | password = `"aaaaaaaa"`, username = `"test"` | `PASSWORD_WEAK` |
| 4 | `validate_whenOnlyTwoCharTypes_shouldReturnWeak` | password = `"aaaaaaAA"`, username = `"test"` | `PASSWORD_WEAK` |
| 5 | `validate_whenContainsUsername_shouldReturnContainsUsername` | password = `"Abcd1234!test"`, username = `"Test"` | `PASSWORD_CONTAINS_USERNAME` |
| 6 | `validate_whenMeetsAllRequirements_shouldReturnNull` | password = `"Abcd1234!"`, username = `"test"` | `null` |
| 7 | `validate_whenThreeCharTypesWithSpecial_shouldReturnNull` | password = `"abcABC!@#"`, username = `"test"` | `null` |
| 8 | `validate_whenThreeCharTypesWithDigit_shouldReturnNull` | password = `"abcABC12345"`, username = `"test"` | `null` |
| 9 | `validate_whenThreeCharTypesWithUpper_shouldReturnNull` | password = `"abc123!@#"`, username = `"test"` | `null` |

**测试关键细节**：
- 用例 2：重复串 `"a1!"` 重复 64 次，总长度 = 64 × 3 = 192 > 64，触发长度上限
- 用例 3：`"aaaaaaaa"` 仅包含小写字母（1 种字符类型），触发 WEAK
- 用例 4：`"aaaaaaAA"` 仅包含小写和大写字母（2 种字符类型），触发 WEAK

### PasswordChangeServiceImplTest

**形态**：class（JUnit 5 + Mockito），mock `UserRepository` 和 `User`。

**包路径**：`com.aimedical.modules.commonmodule.auth.password`

**Mock 配置**：`UserRepository userRepository = mock(UserRepository.class);`，通过构造器注入 `PasswordChangeServiceImpl`。

**测试方法清单**（6 用例）：

| # | 测试方法 | Arrange | Verify |
|---|---------|---------|--------|
| 1 | `isChangeRequired_whenUserExistsAndFlagTrue_shouldReturnTrue` | `userRepository.findById(1L)` → `Optional.of(user)`, `user.getPasswordChangeRequired()` → `true` | `assertTrue(result)` |
| 2 | `isChangeRequired_whenUserExistsAndFlagFalse_shouldReturnFalse` | `userRepository.findById(1L)` → `Optional.of(user)`, `user.getPasswordChangeRequired()` → `false` | `assertFalse(result)` |
| 3 | `isChangeRequired_whenUserNotFound_shouldReturnFalse` | `userRepository.findById(999L)` → `Optional.empty()` | `assertFalse(result)` |
| 4 | `markChangeRequired_shouldSetFlagAndSave` | `userRepository.findById(1L)` → `Optional.of(user)` | `verify(user).setPasswordChangeRequired(true); verify(userRepository).save(user)` |
| 5 | `clearChangeRequired_shouldClearFlagAndSave` | `userRepository.findById(1L)` → `Optional.of(user)` | `verify(user).setPasswordChangeRequired(false); verify(userRepository).save(user)` |
| 6 | `markChangeRequired_whenUserNotFound_shouldSkipSilently` | `userRepository.findById(999L)` → `Optional.empty()` | `verify(userRepository, never()).save(any());` 无异常抛出 |

## 验证

测试命令：
```bash
mvn test -pl modules/common-module/common-module-impl -am -Dtest="PasswordPolicyImplTest,PasswordChangeServiceImplTest" -Dsurefire.failIfNoSpecifiedTests=false
```

## 修订说明（v14 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] 测试用例 4 输入数据 "aaaaaaA1" 包含 3 种字符类型（小写+大写+数字），无法触发 PASSWORD_WEAK | 将 password 改为 "aaaaaaAA"（仅小写+大写，2 种字符类型），使其正确触发 PASSWORD_WEAK；同步更新"测试关键细节"中的用例 4 说明 |
| [一般] 设计文档第 324–359 行包含流式写作注记，非正式规格内容 | 删除全部写作注记段落，仅保留正式的用例说明 |
