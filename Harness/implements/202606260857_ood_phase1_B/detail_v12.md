# 详细设计（v12）

## 概述

在 `common-module-api/auth/` 下新建 `CurrentUser` 接口，作为当前登录用户的轻量级类型化访问器，消除 Controller 层对 `SecurityContextHolder` 的直接操作；在 `common-module-impl/auth/security/` 下新建 `@Component` 实现 `CurrentUserImpl`，从 `SecurityContextHolder` 提取认证信息，并通过 `UserRepository` 查询用户详情。

## 文件规划

| 文件路径（相对 `AIMedical/backend/`） | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/auth/CurrentUser.java` | 新建 | 当前登录用户类型化访问器接口，位于 API 层供所有模块引用 |
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/CurrentUserImpl.java` | 新建 | `@Component` 实现，从 SecurityContextHolder 提取 userId，通过 UserRepository 查询 username/userType |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/CurrentUserImplTest.java` | 新建 | 5 个单元测试，Mockito mockStatic SecurityContextHolder |

## 类型定义

### CurrentUser

**形态**：`public interface`
**包路径**：`com.aimedical.modules.commonmodule.auth`
**职责**：当前登录用户的轻量级类型化访问器，消除 Controller 层对 `SecurityContextHolder` 的直接操作。

```java
package com.aimedical.modules.commonmodule.auth;

import com.aimedical.modules.commonmodule.api.UserType;

public interface CurrentUser {
    Long getUserId();
    String getUsername();
    UserType getUserType();
}
```

**公开接口**：

| 方法签名 | 返回 | 说明 |
|---------|------|------|
| `Long getUserId()` | `Long` | 返回当前认证用户 ID；无认证时返回 null |
| `String getUsername()` | `String` | 返回当前认证用户用户名；无认证或用户不存在时返回 null |
| `UserType getUserType()` | `UserType` | 返回当前认证用户类型枚举；无认证或用户不存在时返回 null |

**构造方式**：无（接口），由 `CurrentUserImpl` 通过 `@Component` 提供实例。

**类型关系**：无继承/实现关系，独立接口定义在 `common-module-api` 层。

---

### CurrentUserImpl

**形态**：`@Component` class，实现 `CurrentUser` 接口
**包路径**：`com.aimedical.modules.commonmodule.auth.security`
**职责**：`CurrentUser` 接口的 `@Component` 实现，从 `SecurityContextHolder` 提取认证信息，通过 `UserRepository` 查询用户附加信息。

```java
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
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
            return null;
        }
        return userId;
    }

    @Override
    public String getUsername() {
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
```

**公开接口**：

| 方法签名 | 返回 | 说明 |
|---------|------|------|
| `Long getUserId()` | `Long` | 从 SecurityContextHolder 提取 principal（Long userId）；无认证或 principal 非 Long 时返回 null |
| `String getUsername()` | `String` | 调用 getUserId()，非 null 时通过 UserRepository.findById 查询 User.getUsername() |
| `UserType getUserType()` | `UserType` | 调用 getUserId()，非 null 时通过 UserRepository.findById 查询 User.getUserType() |

**构造方式**：通过 `@Component` + 构造器注入 `UserRepository`，由 Spring 扫描后自动创建单例。

**类型关系**：实现 `CurrentUser`（`common-module-api` 层接口）。

---

### CurrentUserImplTest

**形态**：class（JUnit 5），纯单元测试，使用 Mockito mockStatic 模拟 SecurityContextHolder
**包路径**：`com.aimedical.modules.commonmodule.auth.security.CurrentUserImplTest`

**测试夹具**：

```java
class CurrentUserImplTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final CurrentUserImpl currentUser = new CurrentUserImpl(userRepository);
    private final MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class);
}
```

**测试方法清单**（5 用例）：

| # | 测试方法 | 场景设置 | 验证点 |
|---|---------|---------|--------|
| 1 | `getUserId_whenAuthenticated_shouldReturnUserId` | `SecurityContextHolder.getContext().getAuthentication()` 返回已认证 Authentication，`getPrincipal()` 返回 `1L` | `1L` |
| 2 | `getUserId_whenNoAuth_shouldReturnNull` | `SecurityContextHolder.getContext().getAuthentication()` 返回 null | `null` |
| 3 | `getUsername_whenAuthenticated_shouldReturnUsername` | 同用例 1 设置，`userRepository.findById(1L)` 返回 `Optional.of(user)`，`user.getUsername()` 返回 `"doctor001"` | `"doctor001"` |
| 4 | `getUserType_whenAuthenticated_shouldReturnUserType` | 同用例 1 设置，`userRepository.findById(1L)` 返回 `Optional.of(user)`，`user.getUserType()` 返回 `UserType.DOCTOR` | `UserType.DOCTOR` |
| 5 | `getUsername_whenUserNotFound_shouldReturnNull` | 同用例 1 设置，`userRepository.findById(1L)` 返回 `Optional.empty()` | `null` |

**测试关键细节**：
- 使用 `Mockito.mockStatic(SecurityContextHolder.class)` 模拟静态方法调用，需在 `@AfterEach` 或 `@AfterAll` 中 `close()` MockedStatic 资源，或使用 try-with-resources 模式确保释放
- 用例 2：同时验证 Authentication 为 null 的场景；另需覆盖 principal 非 Long 类型的边界场景（本设计按任务约定统一在各方法返回 null，测试用例仅覆盖 null Authentication 一种无认证路径，principal 非 Long 的边界已在实现中覆盖但测试用例以 null Authentication 为代表）
- `UserRepository.findById(Long)` 继承自 `JpaRepository`，返回 `Optional<User>`

## 错误处理

CurrentUser/CurrentUserImpl 不抛出业务异常。所有失败路径（无认证、principal 类型不匹配、用户不存在）均以返回 null 值静默处理。错误传播方式：
- `getUserId()`：无认证或 principal 非 Long → 返回 null
- `getUsername()`：getUserId() 返回 null 或 UserRepository.findById 返回 Optional.empty() → 返回 null
- `getUserType()`：getUserId() 返回 null 或 UserRepository.findById 返回 Optional.empty() → 返回 null

## 行为契约

### 前置 / 后置条件

| 方法 | 前置条件 | 后置条件 |
|------|---------|---------|
| `getUserId()` | 无 | 返回 `Long`（userId）或 `null`；不修改 SecurityContext |
| `getUsername()` | 无 | 返回用户名 `String` 或 `null`；不修改数据库 |
| `getUserType()` | 无 | 返回 `UserType` 枚举或 `null`；不修改数据库 |

### 方法调用顺序 / 状态变化规则

- 三个方法相互独立，可任意顺序调用
- `getUsername()` 和 `getUserType()` 内部调用 `getUserId()` 获取 userId，再通过 `userRepository.findById(userId)` 查询，因此一次调用可能触发一次数据库查询
- 无认证场景：`getUserId()` → `null`，`getUsername()` → `null`，`getUserType()` → `null`

### 方法安全契约矩阵

| 方法 | SecurityContext 已认证（principal instanceof Long） | SecurityContext 未认证 / Authentication 为 null | principal 非 Long 类型 |
|------|---------------------------------------------------|-----------------------------------------------|-----------------------|
| `getUserId()` | 返回 principal（Long userId） | 返回 null | 返回 null |
| `getUsername()` | 返回 User.getUsername()；用户不存在返回 null | 返回 null | 返回 null |
| `getUserType()` | 返回 User.getUserType()；用户不存在返回 null | 返回 null | 返回 null |

## 依赖关系

### 本任务新建类型
| 类型 | 所在模块 | 说明 |
|------|---------|------|
| `CurrentUser` | `common-module-api`（`com.aimedical.modules.commonmodule.auth`） | 接口，新建 |
| `CurrentUserImpl` | `common-module-impl`（`com.aimedical.modules.commonmodule.auth.security`） | 实现，新建 |

### 依赖的已有类型
| 类型 | 所在模块 | 说明 |
|------|---------|------|
| `UserType` | `common-module-api`（`com.aimedical.modules.commonmodule.api.UserType`） | 用户类型枚举，`CurrentUser.getUserType()` 返回类型 |
| `User` | `common-module-impl`（`com.aimedical.modules.commonmodule.permission.User`） | 用户实体，`CurrentUserImpl` 通过 `UserRepository` 查询 |
| `UserRepository` | `common-module-impl`（`com.aimedical.modules.commonmodule.permission.UserRepository`） | Spring Data JPA Repository，`findById(Long)` 继承自 `JpaRepository`，返回 `Optional<User>` |

### 框架依赖
| 类型 | 说明 |
|------|------|
| `org.springframework.security.core.Authentication` | SecurityContext 中的认证对象 |
| `org.springframework.security.core.context.SecurityContextHolder` | 静态安全上下文持有者 |
| `org.springframework.stereotype.Component` | CurrentUserImpl 的 Spring 组件注解 |

### 暴露给后续任务的公开接口
- `CurrentUser` 接口（API 层）— AuthController 等业务模块可通过 `@Autowired CurrentUser currentUser` 注入使用，无需依赖 `common-module-impl`
- `CurrentUserImpl` — 由 Spring 自动扫描为 `@Component`，容器中作为 `CurrentUser` 的唯一实现注入

## 单元测试设计

### CurrentUserImplTest

**形态**：class（JUnit 5），纯单元测试，Mockito mockStatic
**包路径**：`com.aimedical.modules.commonmodule.auth.security`

**MockedStatic 生命周期管理**：
- 使用 `MockedStatic<SecurityContextHolder> securityContextHolderMock = mockStatic(SecurityContextHolder.class)` 在测试类级别创建
- 在 `@AfterEach` 或 `@AfterAll` 中调用 `securityContextHolderMock.close()` 释放资源，确保不影响其他测试
- 推荐在 `@BeforeEach` 中重新创建 mock（reset）或采用 try-with-resources 模式在每个测试方法内
- Mock 的 SecurityContextHolder 需配置：`SecurityContextHolder.getContext()` 返回 mock `SecurityContext`，`context.getAuthentication()` 返回 mock `Authentication`，`auth.getPrincipal()` 返回预期值

**测试用例详情**：

1. **`getUserId_whenAuthenticated_shouldReturnUserId`**
   - Arrange: mock `SecurityContextHolder.getContext().getAuthentication().getPrincipal()` 返回 `1L`
   - Act: `currentUser.getUserId()`
   - Assert: `assertEquals(1L, result)`

2. **`getUserId_whenNoAuth_shouldReturnNull`**
   - Arrange: mock `SecurityContextHolder.getContext().getAuthentication()` 返回 `null`
   - Act: `currentUser.getUserId()`
   - Assert: `assertNull(result)`

3. **`getUsername_whenAuthenticated_shouldReturnUsername`**
   - Arrange: mock Authentication.getPrincipal() 返回 `1L`；`userRepository.findById(1L)` 返回 `Optional.of(user)`，`user.getUsername()` 返回 `"doctor001"`
   - Act: `currentUser.getUsername()`
   - Assert: `assertEquals("doctor001", result)`

4. **`getUserType_whenAuthenticated_shouldReturnUserType`**
   - Arrange: mock Authentication.getPrincipal() 返回 `1L`；`userRepository.findById(1L)` 返回 `Optional.of(user)`，`user.getUserType()` 返回 `UserType.DOCTOR`
   - Act: `currentUser.getUserType()`
   - Assert: `assertEquals(UserType.DOCTOR, result)`

5. **`getUsername_whenUserNotFound_shouldReturnNull`**
   - Arrange: mock Authentication.getPrincipal() 返回 `1L`；`userRepository.findById(1L)` 返回 `Optional.empty()`
   - Act: `currentUser.getUsername()`
   - Assert: `assertNull(result)`
