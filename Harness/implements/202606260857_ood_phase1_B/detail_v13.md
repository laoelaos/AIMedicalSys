# 详细设计（v13）

## 概述

在 `common-module-api` 的 `auth` 包中新建 `UserFacade` 接口和迁移后的 `UserInfoResponse` record DTO；在 `common-module-impl` 的 `auth` 包中新建 `UserFacadeImpl` `@Component` 实现。为业务模块提供类型化的用户数据门面访问，消除对 `common-module-impl/permission/User` 实体的直接编译期依赖。同时将原 `dto/response/UserInfoResponse` 从 impl 模块迁至 api 模块的 `auth` 包下，所有引用方同步更新 import。

## 文件规划

| 文件路径（相对 `AIMedical/backend/`） | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/auth/UserInfoResponse.java` | 新建（实际为迁移） | `UserInfoResponse` record DTO，从 impl 模块 `dto/response/` 迁至 api 模块 `auth/` |
| `modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/auth/UserFacade.java` | 新建 | 用户数据门面接口，API 层供所有业务模块引用 |
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/UserFacadeImpl.java` | 新建 | `@Component` 实现，注入 `UserRepository`，提供用户数据查询 |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/UserFacadeImplTest.java` | 新建 | 6 个单元测试，Mockito mock UserRepository |
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/response/UserInfoResponse.java` | 删除 | 已迁移至 api 模块 |
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/response/LoginResponse.java` | 修改 | update import: `dto.response.UserInfoResponse` → `auth.UserInfoResponse` |
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/AuthService.java` | 修改 | update import |
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/AuthServiceImpl.java` | 修改 | update import（`dto.response.UserInfoResponse` → `auth.UserInfoResponse`）|
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/controller/AuthController.java` | 修改 | update import |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/AuthServiceTest.java` | 修改 | update import |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/controller/AuthControllerTest.java` | 修改 | update import |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/dto/response/UserInfoResponseTest.java` | 修改 | update import（包路径变更后测试仍在原处，但引用的类包路径已变） |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/dto/response/LoginResponseTest.java` | 修改 | update import |

## 类型定义

### UserInfoResponse

**形态**：`public record`
**包路径**：`com.aimedical.modules.commonmodule.auth`
**职责**：用户信息 DTO，携带 id、用户名、真实姓名、联系方式、角色、岗位、权限集合。与前端 `UserInfo` 接口字段名对齐。

```java
package com.aimedical.modules.commonmodule.auth;

import java.util.Set;

public record UserInfoResponse(
    Long id,
    String username,
    String realName,
    String phone,
    String email,
    String role,
    String position,
    Set<String> permissions
) {}
```

**公开接口**：
| 方法签名 | 返回 | 说明 |
|---------|------|------|
| `Long id()` | `Long` | 用户 ID |
| `String username()` | `String` | 用户名 |
| `String realName()` | `String` | 真实姓名（对应 `User.nickname`） |
| `String phone()` | `String` | 手机号（可 null） |
| `String email()` | `String` | 邮箱（可 null） |
| `String role()` | `String` | 用户主角色 code（按 Role.sort 升序取第一个；无角色时返回空字符串） |
| `String position()` | `String` | 用户岗位 code（取第一个岗位；无岗位时返回空字符串） |
| `Set<String> permissions()` | `Set<String>` | 权限码集合（从 roles → posts → functions 收集去重） |

**构造方式**：通过 record 构造器直接实例化。

**类型关系**：无继承/实现关系，独立 record 定义在 `common-module-api` 层。原位置 `dto/response/UserInfoResponse.java` 被删除。

---

### UserFacade

**形态**：`public interface`
**包路径**：`com.aimedical.modules.commonmodule.auth`
**职责**：统一用户数据访问门面，供业务模块查询用户完整信息而不直接依赖 `common-module-impl/permission/User` 实体。与 `CurrentUser` 职责分工：`CurrentUser` 提供当前登录用户的轻量身份标识（SecurityContext 驱动的会话级访问），`UserFacade` 提供任意用户的完整业务数据（Repository 驱动的数据级访问）。

```java
package com.aimedical.modules.commonmodule.auth;

public interface UserFacade {
    UserInfoResponse findById(Long userId);
    UserInfoResponse findByUsername(String username);
    boolean existsById(Long userId);
}
```

**公开接口**：
| 方法签名 | 返回 | 说明 |
|---------|------|------|
| `UserInfoResponse findById(Long userId)` | `UserInfoResponse` | 按 ID 查询用户完整信息；用户不存在返回 null |
| `UserInfoResponse findByUsername(String username)` | `UserInfoResponse` | 按用户名查询用户完整信息；用户不存在返回 null |
| `boolean existsById(Long userId)` | `boolean` | 检查用户是否存在 |

**构造方式**：无（接口），由 `UserFacadeImpl` 通过 `@Component` 提供实例。

**类型关系**：无继承/实现关系，独立接口定义在 `common-module-api` 层。

---

### UserFacadeImpl

**形态**：`@Component` class，实现 `UserFacade` 接口
**包路径**：`com.aimedical.modules.commonmodule.auth`
**职责**：`UserFacade` 接口的 `@Component` 实现，内部注入 `UserRepository`，将 `User` 实体转换为 `UserInfoResponse` DTO。

```java
package com.aimedical.modules.commonmodule.auth;

import com.aimedical.modules.commonmodule.permission.PermissionFunction;
import com.aimedical.modules.commonmodule.permission.Post;
import com.aimedical.modules.commonmodule.permission.Role;
import com.aimedical.modules.commonmodule.permission.User;
import com.aimedical.modules.commonmodule.permission.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserFacadeImpl implements UserFacade {
    private final UserRepository userRepository;

    public UserFacadeImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
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
        String role = resolvePrimaryRole(user);
        String position = resolvePosition(user);
        Set<String> permissions = resolvePermissions(user);
        return new UserInfoResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getPhone(),
                user.getEmail(),
                role,
                position,
                permissions
        );
    }

    private String resolvePrimaryRole(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return "";
        }
        return user.getRoles().stream()
                .filter(Role::getEnabled)
                .min(Comparator.comparing(Role::getSort, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(Role::getCode)
                .orElse("");
    }

    private String resolvePosition(User user) {
        if (user.getPosts() == null || user.getPosts().isEmpty()) {
            return "";
        }
        return user.getPosts().stream()
                .findFirst()
                .map(Post::getCode)
                .orElse("");
    }

    private Set<String> resolvePermissions(User user) {
        Set<String> permissions = new HashSet<>();
        // Collect from roles → posts → functions
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                if (role.getPosts() != null) {
                    for (Post post : role.getPosts()) {
                        if (post.getFunctions() != null) {
                            post.getFunctions().stream()
                                    .filter(f -> Boolean.TRUE.equals(f.getEnabled()))
                                    .map(PermissionFunction::getCode)
                                    .forEach(permissions::add);
                        }
                    }
                }
            }
        }
        // Collect from direct posts → functions
        if (user.getPosts() != null) {
            for (Post post : user.getPosts()) {
                if (post.getFunctions() != null) {
                    post.getFunctions().stream()
                            .filter(f -> Boolean.TRUE.equals(f.getEnabled()))
                            .map(PermissionFunction::getCode)
                            .forEach(permissions::add);
                }
            }
        }
        return permissions;
    }
}
```

**公开接口**：
| 方法签名 | 返回 | 说明 |
|---------|------|------|
| `UserInfoResponse findById(Long userId)` | `UserInfoResponse` | 委托 `userRepository.findById(id)`，存在则转换，不存在返回 null |
| `UserInfoResponse findByUsername(String username)` | `UserInfoResponse` | 委托 `userRepository.findByUsername(username)`，存在则转换，不存在返回 null |
| `boolean existsById(Long userId)` | `boolean` | 委托 `userRepository.existsById(id)` |

**构造方式**：通过 `@Component` + 构造器注入 `UserRepository`，由 Spring 扫描后自动创建单例。

**类型关系**：实现 `UserFacade`（`common-module-api` 层接口）。

---

### UserFacadeImplTest

**形态**：class（JUnit 5），纯单元测试，Mockito mock `UserRepository`
**包路径**：`com.aimedical.modules.commonmodule.auth.UserFacadeImplTest`

**测试夹具**：
```java
class UserFacadeImplTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserFacadeImpl userFacade = new UserFacadeImpl(userRepository);
}
```

**测试方法清单**（6 用例）：

| # | 测试方法 | 场景设置 | 验证点 |
|---|---------|---------|--------|
| 1 | `findById_whenUserExists_shouldReturnUserInfo` | `userRepository.findById(1L)` 返回 `Optional.of(user)`，user 携带完整的 roles/posts 和 functions 关联 | 返回完整的 `UserInfoResponse`，所有字段正确映射 |
| 2 | `findById_whenUserNotFound_shouldReturnNull` | `userRepository.findById(999L)` 返回 `Optional.empty()` | `assertNull(result)` |
| 3 | `findByUsername_whenUserExists_shouldReturnUserInfo` | `userRepository.findByUsername("doctor001")` 返回 `Optional.of(user)` | 返回完整的 `UserInfoResponse`，字段正确映射 |
| 4 | `findByUsername_whenUserNotFound_shouldReturnNull` | `userRepository.findByUsername("nobody")` 返回 `Optional.empty()` | `assertNull(result)` |
| 5 | `existsById_whenUserExists_shouldReturnTrue` | `userRepository.existsById(1L)` 返回 `true` | `assertTrue(result)` |
| 6 | `existsById_whenUserNotFound_shouldReturnFalse` | `userRepository.existsById(999L)` 返回 `false` | `assertFalse(result)` |

**测试关键细节**：
- 使用 `Mockito.mock(UserRepository.class)` 创建 mock，无需 mockStatic
- 用例 1 和 3 中 `User` 实体需 stub：
  - `user.getId()` → 1L
  - `user.getUsername()` → `"doctor001"`
  - `user.getNickname()` → `"张医生"`
  - `user.getPhone()` → `"13800138000"`
  - `user.getEmail()` → `"doctor@example.com"`
  - `user.getRoles()` → `Set<Role>`，其中 Role stub `getCode()` → `"DOCTOR"`，`getSort()` → 0，`getEnabled()` → true
  - `user.getPosts()` → `Set<Post>`，其中 Post stub `getCode()` → `"OUTPATIENT"`，`getFunctions()` → `Set<PermissionFunction>`，function stub `getCode()` → `"patient:view"`，`getEnabled()` → true
- 权限收集验证：assert 结果 permissions 包含从 roles 级联和 posts 级联收集的权限码

## 错误处理

`UserFacade`/`UserFacadeImpl` 不抛出业务异常。所有失败路径（用户不存在）以返回 null 值静默处理：

- `findById(Long)`：用户不存在 → 返回 null
- `findByUsername(String)`：用户不存在 → 返回 null
- `existsById(Long)`：始终返回 boolean，不会失败

## 行为契约

### 前置 / 后置条件

| 方法 | 前置条件 | 后置条件 |
|------|---------|---------|
| `findById(Long userId)` | userId 可为 null（此时返回 null） | 返回 `UserInfoResponse` 或 `null`；不修改数据库 |
| `findByUsername(String username)` | username 可为 null（此时返回 null） | 返回 `UserInfoResponse` 或 `null`；不修改数据库 |
| `existsById(Long userId)` | userId 可为 null（此时返回 false） | 返回 `boolean`；不修改数据库 |

### 方法调用顺序 / 状态变化规则

- 三个方法相互独立，可任意顺序调用
- 每次调用触发一次 Repository 查询
- 实现无状态，不维护缓存

## 依赖关系

### 本任务新建类型

| 类型 | 所在模块 | 说明 |
|------|---------|------|
| `UserInfoResponse` | `common-module-api`（`com.aimedical.modules.commonmodule.auth`） | record DTO，从 impl 迁移至 api |
| `UserFacade` | `common-module-api`（`com.aimedical.modules.commonmodule.auth`） | 接口，新建 |
| `UserFacadeImpl` | `common-module-impl`（`com.aimedical.modules.commonmodule.auth`） | 实现，新建 |
| `UserFacadeImplTest` | `common-module-impl` test（`com.aimedical.modules.commonmodule.auth`） | 测试，新建 |

### 依赖的已有类型

| 类型 | 所在模块 | 说明 |
|------|---------|------|
| `User` | `common-module-impl/permission/` | 用户实体，`UserFacadeImpl.toUserInfoResponse()` 从中提取所有字段 |
| `UserRepository` | `common-module-impl/permission/` | Spring Data JPA Repository，提供 `findById`、`findByUsername`、`existsById` |
| `Role` | `common-module-impl/permission/` | 角色实体，用于 `resolvePrimaryRole` 取主角色 code 和 `resolvePermissions` 级联收集权限 |
| `Post` | `common-module-impl/permission/` | 岗位实体，用于 `resolvePosition` 取岗位 code 和级联收集权限 |
| `PermissionFunction` | `common-module-impl/permission/` | 功能实体，`resolvePermissions` 中收集权限码 |

### 框架依赖

| 类型 | 说明 |
|------|------|
| `org.springframework.stereotype.Component` | `UserFacadeImpl` 的 Spring 组件注解 |

### 暴露给后续任务的公开接口

- `UserFacade` 接口（API 层）— 业务模块可通过 `@Autowired UserFacade userFacade` 注入使用，无需依赖 `common-module-impl`
- `UserFacadeImpl` — 由 Spring 自动扫描为 `@Component`，容器中作为 `UserFacade` 的唯一实现注入
- `UserInfoResponse`（API 层）— 业务模块可引用此 DTO 类型，无需依赖 impl 模块

### 引用变更清单

以下文件需要将 `import com.aimedical.modules.commonmodule.dto.response.UserInfoResponse` 改为 `import com.aimedical.modules.commonmodule.auth.UserInfoResponse`：

| 文件 | 路径 |
|------|------|
| `LoginResponse.java` | `modules/common-module/common-module-impl/src/main/java/.../dto/response/LoginResponse.java` |
| `AuthService.java` | `modules/common-module/common-module-impl/src/main/java/.../service/AuthService.java` |
| `AuthServiceImpl.java` | `modules/common-module/common-module-impl/src/main/java/.../service/impl/AuthServiceImpl.java` |
| `AuthController.java` | `modules/common-module/common-module-impl/src/main/java/.../controller/AuthController.java` |
| `AuthServiceTest.java` | `modules/common-module/common-module-impl/src/test/java/.../service/AuthServiceTest.java` |
| `AuthControllerTest.java` | `modules/common-module/common-module-impl/src/test/java/.../controller/AuthControllerTest.java` |
| `UserInfoResponseTest.java` | `modules/common-module/common-module-impl/src/test/java/.../dto/response/UserInfoResponseTest.java` |
| `LoginResponseTest.java` | `modules/common-module/common-module-impl/src/test/java/.../dto/response/LoginResponseTest.java` |

## 单元测试设计

### UserFacadeImplTest

**形态**：class（JUnit 5），纯单元测试，Mockito mock
**包路径**：`com.aimedical.modules.commonmodule.auth`

**Mock 配置**：`UserRepository userRepository = mock(UserRepository.class);`，通过构造器注入 `UserFacadeImpl`。

**测试数据**：
- User mock: id=1L, username="doctor001", nickname="张医生", phone="13800138000", email="doctor@example.com"
- Role mock: code="DOCTOR", sort=0, enabled=true, posts=Set.of(post)
- Post mock: code="OUTPATIENT", functions=Set.of(function)
- PermissionFunction mock: code="patient:view", enabled=true

**测试用例详情**：

1. **`findById_whenUserExists_shouldReturnUserInfo`**
   - Arrange: `userRepository.findById(1L)` → `Optional.of(user)`，user stub 返回完整数据；Role stub `getEnabled()` → true；Post stub `getFunctions()` → function set
   - Act: `userFacade.findById(1L)`
   - Assert: `assertNotNull(result)`；`assertEquals("doctor001", result.username())`；`assertEquals("张医生", result.realName())`；`assertEquals("DOCTOR", result.role())`；`assertEquals("OUTPATIENT", result.position())`；`assertTrue(result.permissions().contains("patient:view"))`

2. **`findById_whenUserNotFound_shouldReturnNull`**
   - Arrange: `userRepository.findById(999L)` → `Optional.empty()`
   - Act: `userFacade.findById(999L)`
   - Assert: `assertNull(result)`

3. **`findByUsername_whenUserExists_shouldReturnUserInfo`**
   - Arrange: `userRepository.findByUsername("doctor001")` → `Optional.of(user)`，user stub 同用例 1
   - Act: `userFacade.findByUsername("doctor001")`
   - Assert: 同用例 1 断言

4. **`findByUsername_whenUserNotFound_shouldReturnNull`**
   - Arrange: `userRepository.findByUsername("nobody")` → `Optional.empty()`
   - Act: `userFacade.findByUsername("nobody")`
   - Assert: `assertNull(result)`

5. **`existsById_whenUserExists_shouldReturnTrue`**
   - Arrange: `userRepository.existsById(1L)` → `true`
   - Act: `userFacade.existsById(1L)`
   - Assert: `assertTrue(result)`

6. **`existsById_whenUserNotFound_shouldReturnFalse`**
   - Arrange: `userRepository.existsById(999L)` → `false`
   - Act: `userFacade.existsById(999L)`
   - Assert: `assertFalse(result)`
