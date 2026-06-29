# 详细设计（v1）

## 概述

依据 OOD 文档 `Docs/05_ood_phase1_B.md` 5.1 节实体变更表与 8.1/8.3 节 A1/A3/H1 修复点，完成包 A 数据建模缺陷修复与字段扩展：

- `User` 实体新增 `passwordChangeRequired`、`tokenVersion` 两字段，补齐 5.1 节缺失字段；为 Phase 2 `PasswordChangeCheckFilter` 与 Refresh Token `tokenVersion` 比对提供编译期依赖。
- `Role` 实体补 `enabled` 字段 `@Column(nullable=false)` 并新增 `sort` 字段，为 Phase 3 `UserConverter` 按优先级取主角色提供数据依赖。
- `Post` 实体补 `enabled` 字段 `@Column(nullable=false)`，与 `Role.enabled` 同步收敛布尔字段一致性。
- `UserRepository.findByUsername` 返回类型由 `User` 改为 `Optional<User>`，与既有 `findById` 一致，并同步修复生产/测试调用方。

本任务仅做 Java 实体与 Repository 签名侧的修改；`schema.sql` 的 DDL 同步与新字段的集成测试用例不在本任务范围。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/User.java` | 修改 | 新增 `passwordChangeRequired`、`tokenVersion` 字段及相关 getter/setter/Javadoc |
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Role.java` | 修改 | 为 `enabled` 补 `@Column(nullable=false)`；新增 `sort` 字段及相关 getter/setter |
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/Post.java` | 修改 | 为 `enabled` 补 `@Column(nullable=false)` |
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/permission/UserRepository.java` | 修改 | `findByUsername` 返回类型由 `User` 改为 `Optional<User>` |
| `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/AuthServiceImpl.java` | 修改 | `login()` 中 `findByUsername` 调用方适配为 `Optional.map`/`.orElseThrow` 链式调用 |
| `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/AuthServiceTest.java` | 修改 | 4 处 Mockito stub 返回值改为 `Optional.of(testUser)` / `Optional.empty()` |

## 类型定义

### 字段定义：`User.passwordChangeRequired`

**形态**：实体字段（`@Column` + `Boolean`）
**包路径**：`com.aimedical.modules.commonmodule.permission.User`
**职责**：标记用户是否必须修改密码（首次登录或管理员标记密码过期时为 `true`），由 `PasswordChangeCheckFilter` 在 Phase 2 读取以决定是否阻断请求。

**字段签名**：
```java
/**
 * 是否必须修改密码
 *
 * <p>true 表示首次登录或被管理员标记密码过期，需在 PasswordChangeCheckFilter
 * 阶段强制走 /api/auth/password 流程；默认 false。
 */
@Column(nullable = false, columnDefinition = "BIT(1) DEFAULT 0")
private Boolean passwordChangeRequired = false;
```

**公开接口**：
```java
public Boolean getPasswordChangeRequired();
public void setPasswordChangeRequired(Boolean passwordChangeRequired);
```

**类型关系**：组合于 `User` 实体；与既有 `enabled`、`userType` 字段并列。

---

### 字段定义：`User.tokenVersion`

**形态**：实体字段（`@Column` + `Integer`）
**包路径**：`com.aimedical.modules.commonmodule.permission.User`
**职责**：令牌版本号，Refresh Token claims 携带此版本号；密码变更后递增（+1）以即时撤销旧 Refresh Token。

**字段签名**：
```java
/**
 * 令牌版本号
 *
 * <p>Refresh Token 刷新时与 claims 中的 tokenVersion 比对，不一致即拒绝；
 * 密码变更后递增（+1），使已签发的旧 Refresh Token 即时失效。默认 0。
 */
@Column(nullable = false)
private Integer tokenVersion = 0;
```

**公开接口**：
```java
public Integer getTokenVersion();
public void setTokenVersion(Integer tokenVersion);
```

**类型关系**：组合于 `User` 实体；Phase 2 的 `JwtTokenProvider` 与 `AuthServiceImpl.changePassword()` 读取/递增。

---

### 字段定义：`Role.enabled`

**形态**：实体字段（`@Column` + `Boolean`）
**包路径**：`com.aimedical.modules.commonmodule.permission.Role`
**职责**：标记角色是否启用；DDL `sys_role.enabled` 已为 `NOT NULL DEFAULT 1`，本任务补齐 Java 注解侧。

**字段签名**（修改后）：
```java
@Column(nullable = false)
private Boolean enabled = true;
```

**类型关系**：与 `Post.enabled` 字段保持同款注解风格，遵循 5.1 节"布尔字段 @Column 注解对齐"约定。

---

### 字段定义：`Role.sort`

**形态**：实体字段（`@Column` + `Integer`）
**包路径**：`com.aimedical.modules.commonmodule.permission.Role`
**职责**：角色优先级排序号，值越小优先级越高；Phase 3 `UserConverter` 按此字段排序取主角色。

**字段签名**：
```java
/**
 * 排序号（角色优先级）
 *
 * <p>值越小优先级越高。Phase 3 中由 UserConverter 按此字段排序取用户主角色。
 * 默认 0。
 */
@Column(nullable = false)
private Integer sort = 0;
```

**公开接口**：
```java
public Integer getSort();
public void setSort(Integer sort);
```

**类型关系**：与 `Post.sort`（现有 `private Integer sort;`，未声明 `nullable=false`）保持字段命名一致；本任务仅为 `Role.sort` 加 `@Column(nullable=false)`，`Post.sort` 不在本任务范围。

---

### 字段定义：`Post.enabled`

**形态**：实体字段（`@Column` + `Boolean`）
**包路径**：`com.aimedical.modules.commonmodule.permission.Post`
**职责**：标记岗位是否启用；DDL `sys_post.enabled` 已为 `NOT NULL DEFAULT 1`，本任务补齐 Java 注解侧。

**字段签名**（修改后）：
```java
@Column(nullable = false)
private Boolean enabled = true;
```

---

### Repository 方法：`UserRepository.findByUsername`

**形态**：Spring Data JPA Repository 方法（接口方法）
**包路径**：`com.aimedical.modules.commonmodule.permission.UserRepository`
**职责**：根据用户名查询用户；返回 `Optional<User>` 表达"未找到"语义。

**方法签名**（修改后）：
```java
/**
 * 根据用户名查询用户
 *
 * @param username 用户名
 * @return Optional<User>；未找到时为 Optional.empty()
 */
Optional<User> findByUsername(String username);
```

**类型关系**：扩展 `JpaRepository<User, Long>`；与既有 `findById(Long)` 返回 `Optional<User>` 风格一致。

---

### 调用方适配：`AuthServiceImpl.login`

**形态**：方法体内调用片段
**包路径**：`com.aimedical.modules.commonmodule.service.impl.AuthServiceImpl#login(LoginRequest)`
**职责**：将 `findByUsername` 由 `null` 检查改为 `Optional` 链式调用，复用同文件 `refreshToken/getCurrentUser/updateProfile` 中 `findById(...).orElseThrow(...)` 模式。

**改写规则**（仅替换行 60-65）：
```java
User user = userRepository.findByUsername(request.getUsername())
        .orElseThrow(() -> {
            log.warn("登录失败：用户不存在，用户名: {}", request.getUsername());
            return new BusinessException(GlobalErrorCode.UNAUTHORIZED, "用户名或密码错误");
        });
```

**调用顺序**：与既有 `login()` 方法体保持一致——`enabled` 检查 → `passwordEncoder.matches` → 业务响应构建。

---

### 测试 mock 适配：`AuthServiceTest`

**形态**：Mockito stub 行
**包路径**：`com.aimedical.modules.commonmodule.service.AuthServiceTest`（位于 `LoginTests` 内）
**职责**：4 处 `when(userRepository.findByUsername(...))` 的返回值类型随 Repository 签名变更同步调整。

**stub 改写映射**（精确到行号）：

| 行号 | 现状 | 改写后 |
|------|------|--------|
| 82 | `when(userRepository.findByUsername("testuser")).thenReturn(testUser);` | `when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));` |
| 102 | `when(userRepository.findByUsername("nonexistent")).thenReturn(null);` | `when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());` |
| 117 | `when(userRepository.findByUsername("testuser")).thenReturn(testUser);` | `when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));` |
| 133 | `when(userRepository.findByUsername("testuser")).thenReturn(testUser);` | `when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));` |

**类型关系**：复用文件已 import 的 `java.util.Optional`，不新增 import。

## 错误处理

- **生产侧**：`AuthServiceImpl.login()` 在 `findByUsername(...)` 返回 `Optional.empty()` 时仍抛出 `BusinessException(GlobalErrorCode.UNAUTHORIZED, "用户名或密码错误")`，与原 `if (user == null)` 分支同款错误码与消息，确保登录失败响应与 8.1 节 H1 修复前的外部行为一致（不引入新的安全信息泄露路径）。
- **测试侧**：`AuthServiceTest#shouldThrowExceptionWhenUserNotFound`（行 99-112）断言的错误码仍为 `GlobalErrorCode.UNAUTHORIZED`，无需修改断言。
- **传播策略**：异常经 `@Transactional(readOnly=true)` 包裹下抛出，`BusinessException extends RuntimeException`（见 `AIMedical/backend/common/src/main/java/com/aimedical/common/exception/BusinessException.java:3`），由 `GlobalExceptionHandler` 统一处理；本任务不引入新的异常类型。

## 行为契约

### `User` 实体
- **前置条件**：无。
- **后置条件**：新建 `User` 实例后，`passwordChangeRequired == false`、`tokenVersion == 0`、`enabled == true`（既有）。
- **不变量**：`passwordChangeRequired` 与 `tokenVersion` 与既有字段同等由 JPA `nullable=false` 约束保证非空；`columnDefinition="BIT(1) DEFAULT 0"` 仅在 DDL 生成场景下生效，本任务不触达 `schema.sql`。

### `Role` 实体
- **前置条件**：无。
- **后置条件**：新建 `Role` 实例后，`enabled == true`、`sort == 0`。
- **不变量**：`enabled` 与 `sort` 均受 `@Column(nullable=false)` 约束。

### `Post` 实体
- **前置条件**：无。
- **后置条件**：新建 `Post` 实例后，`enabled == true`。
- **不变量**：`enabled` 受 `@Column(nullable=false)` 约束。

### `UserRepository.findByUsername`
- **前置条件**：`username != null`（调用方约束，JPA 未在 Repository 接口层加校验）。
- **后置条件**：用户存在返回 `Optional.of(user)`，否则返回 `Optional.empty()`，**不再返回 null**。

### `AuthServiceImpl.login`
- **调用顺序**：`findByUsername` → `enabled` 检查 → `passwordEncoder.matches` → 业务响应构建。
- **状态变化**：本任务不修改任何持久化字段；`login()` 仍为只读事务（`@Transactional(readOnly=true)`）。

### `AuthServiceTest`
- **前置条件**：4 处 stub 必须返回 `Optional`，否则 Mockito 报类型不匹配运行时错误。
- **后置条件**：`Optional.of(testUser)` 与 `Optional.empty()` 与 `UserRepository` 新签名在编译期一致。

## 依赖关系

### 已有依赖（仅引用，不修改）
- `com.aimedical.common.base.BaseEntity`：被 `User`/`Role`/`Post` 继承，提供 `id`/`createdAt`/`updatedAt`/`deleted` 字段。
- `com.aimedical.common.exception.BusinessException`：`AuthServiceImpl.login()` 中 `orElseThrow` 抛出此异常。
- `com.aimedical.common.exception.GlobalErrorCode`：`UNAUTHORIZED` 错误码来源。
- `com.aimedical.modules.commonmodule.api.UserType`：`User.userType` 枚举类型。
- `org.springframework.data.jpa.repository.JpaRepository`：`UserRepository` 父接口。
- `java.util.Optional`：Repository 方法返回类型与 `AuthServiceTest` mock 包装类型。

### 暴露给后续任务的公开接口
- `User.passwordChangeRequired`（getter/setter）：Phase 2 `PasswordChangeCheckFilter`（3.3 节）、Phase 2 `AuthServiceImpl.changePassword()`（3.2 节步骤 9）将读取与清除。
- `User.tokenVersion`（getter/setter）：Phase 2 `JwtTokenProvider` 生成 Refresh Token 时嵌入 claims；`AuthServiceImpl.changePassword()` 步骤 8 递增（+1）；`AuthServiceImpl.refreshToken()` 步骤 9 比对。
- `Role.sort`（getter/setter）：Phase 3 `UserConverter`（7.4 节）按 `sort` 升序取主角色，替代原 `primaryRole` 字段引用。
- `UserRepository.findByUsername` 返回 `Optional<User>`：Phase 2 `AuthServiceImpl.login()` 与 Phase 3 `UserFacade.findByUsername(String)` 链式调用入口。

### 不在范围
- `schema.sql` 的 `sys_user.passwordChangeRequired`、`sys_user.token_version`、`sys_role.sort` 列 DDL 变更——由后续 DDL 任务统一处理。
- `PermissionFunction` 重命名（M8，5 个文件级联）——独立任务。
- `User.passwordChangeRequired` / `User.tokenVersion` / `Role.sort` 字段级集成测试用例——由"阶段 4 集成测试任务"统一处理。
- `Post.sort` 字段补 `@Column(nullable=false)`——不在 OOD 5.1 节 Post 变更表内，本任务不处理。