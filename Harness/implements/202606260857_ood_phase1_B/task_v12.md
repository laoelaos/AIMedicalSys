# 任务指令（v12）

## 动作
NEW

## 任务描述

### CurrentUser（`common-module-api` 模块）

在 `common-module-api`（`common-module-api/src/main/java/com/aimedical/modules/commonmodule/auth/`）下新建 `CurrentUser.java` 接口，作为当前登录用户的轻量级类型化访问器，消除 Controller 层对 `SecurityContextHolder` 的直接操作。

**形态**：`public interface`

**方法签名**：

```java
package com.aimedical.modules.commonmodule.auth;

import com.aimedical.modules.commonmodule.api.UserType;

public interface CurrentUser {
    Long getUserId();
    String getUsername();
    UserType getUserType();
}
```

### CurrentUserImpl（`common-module-impl` 模块）

在 `common-module-impl`（`common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/`）下新建 `CurrentUserImpl.java`，作为 `@Component` 实现，从 `SecurityContextHolder` 提取认证信息。

**形态**：`@Component` class，实现 `CurrentUser` 接口

**实现要点**：

1. `getUserId()`：从 `SecurityContextHolder.getContext().getAuthentication().getPrincipal()` 获取 principal（类型为 `Long`，即用户 ID），直接返回
2. `getUsername()`：通过 `UserRepository.findById(userId)` 查询 `User` 实体，返回 `user.getUsername()`
3. `getUserType()`：通过 `UserRepository.findById(userId)` 查询 `User` 实体，返回 `user.getUserType()`
4. 无认证（Authentication 为 null 或 principal 非 Long）时，`getUserId()` 返回 null，`getUsername()`/`getUserType()` 返回 null 或空值

**实现方式**：

```java
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

### 新建文件清单

| 操作 | 文件路径（相对 `AIMedical/backend/`） |
|------|---------|
| 新建 | `modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/auth/CurrentUser.java` |
| 新建 | `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/security/CurrentUserImpl.java` |
| 新建 | `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/security/CurrentUserImplTest.java` |

## 选择理由

- CurrentUser 是 Stage 3 所有业务接口/服务重构的基础抽象，提供 Controller 层对 SecurityContextHolder 的类型化访问，消除重复的 `(Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal()` 模式
- 位于 `common-module-api` 中，业务模块（patient/doctor/admin）可引用而不产生对 `common-module-impl` 的编译期依赖（OOD 2.2 依赖规则）
- 是 AuthController 重构（M4—消除 JwtUtil 依赖）的前置条件
- 依赖链：CurrentUser → AuthController → AuthServiceImpl，先创建 API 层抽象再向下推进
- Stage 2（安全 Filter/限流/黑名单）已在 R4-R11 全部完成，Stage 3 当前为最高优先级

## 任务上下文

### OOD 引用

- **1.3 核心抽象**：`CurrentUser` 接口定义为"当前登录用户的轻量级类型化访问器，消除 Controller 层对 SecurityContextHolder 的直接操作"
- **8.1 M4**：`AuthController 依赖 JwtUtil` — 引入 CurrentUser 接口，Controller 通过 SecurityContext 获取
- **2.1 目录结构**：`CurrentUser.java` 位于 `common-module-api/.../auth/`，`CurrentUserImpl.java` 位于 `common-module-impl/.../auth/security/`

### 方法签名与行为契约

| 方法 | 安全上下文已认证 | 无认证 / principal 非预期类型 |
|------|-----------------|------------------------------|
| `getUserId()` | 返回 principal（`Long userId`） | 返回 `null` |
| `getUsername()` | 返回用户名字符串 | 返回 `null` |
| `getUserType()` | 返回用户类型枚举 | 返回 `null` |

### 与 UserFacade 的职责分工

- `CurrentUser`：提供当前登录用户的轻量身份标识（userId/username/userType），SecurityContext 驱动的会话级访问
- `UserFacade`（后续任务）：提供任意用户的完整业务数据（昵称/手机号/邮箱等），Repository 驱动的数据级访问

## 已有代码上下文

### 依赖的已有类型

| 类型 | 所在模块 | 说明 |
|------|---------|------|
| `UserType` | `common-module-api`（`com.aimedical.modules.commonmodule.api.UserType`） | 用户类型枚举，已存在 |
| `User` | `common-module-impl`（`com.aimedical.modules.commonmodule.permission.User`） | 用户实体，R1 扩展 |
| `UserRepository` | `common-module-impl`（`com.aimedical.modules.commonmodule.permission.UserRepository`） | 返回 `Optional<User>`，R1 适配 |
| `SecurityContextHolder` | Spring Security | 标准安全上下文 API |

### 包路径确认

```
common-module-api/src/main/java/com/aimedical/modules/commonmodule/
    api/
        UserType.java          # 已存在
    auth/
        CurrentUser.java       # 待新建

common-module-impl/src/main/java/com/aimedical/modules/commonmodule/
    auth/security/
        CurrentUserImpl.java   # 待新建（与其他 Filter/Handler 同包）
    permission/
        User.java              # 已存在
        UserRepository.java    # 已存在
```

### 测试设计

**CurrentUserImplTest**（JUnit 5，Mockito）

| # | 测试方法 | 场景 | 验证点 |
|---|---------|------|--------|
| 1 | `getUserId_whenAuthenticated_shouldReturnUserId` | Mock SecurityContext 返回已认证 Authentication（principal = 1L） | `1L` |
| 2 | `getUserId_whenNoAuth_shouldReturnNull` | SecurityContext 返回 null Authentication | `null` |
| 3 | `getUsername_whenAuthenticated_shouldReturnUsername` | Mock UserRepository 返回用户名 "doctor001" | `"doctor001"` |
| 4 | `getUserType_whenAuthenticated_shouldReturnUserType` | Mock UserRepository 返回 UserType.DOCTOR | `UserType.DOCTOR` |
| 5 | `getUsername_whenUserNotFound_shouldReturnNull` | Mock UserRepository 返回 `Optional.empty()` | `null` |

使用 Mockito 的 `mockStatic(SecurityContextHolder.class)` 模拟静态方法调用。

### 成功标准

- `mvn compile test-compile -pl modules/common-module -am` 通过
- `CurrentUserImplTest` 新建用例全部通过
- 不影响已有 258 个通过用例

## 修订说明（v12 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] 计划中新建文件清单遗漏测试文件 | 任务文件内容已正确包含测试文件清单，无需修改；已同步修正 plan.md |
| [一般] 计划中测试用例数量描述不精确 | 任务文件内容已精确列明 5 个测试方法及名称，无需修改；已同步修正 plan.md |
