# 详细设计（v16）

## 概述

修复 T9：UserConverter 补充三处过滤逻辑（禁用角色过滤、null-safe sort、禁用权限过滤），并将 UserFacadeImpl 的转换职责委托给 UserConverter，删除冗余私有方法。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/converter/UserConverter.java` | 修改 | `resolveRole()` 补充 `.filter(Role::getEnabled)` + null-safe sort；`resolvePermissions()` 两层循环补充 `PermissionFunction::getEnabled` 过滤 |
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/UserFacadeImpl.java` | 修改 | 注入 `UserConverter`，`toUserInfoResponse()` 委托给 `userConverter`，删除三个私有方法 |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/converter/UserConverterTest.java` | 修改 | 新增禁用角色/null sort/禁用权限三个测试 |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/UserFacadeImplTest.java` | 修改 | 注入 `UserConverter` mock，验证委托行为 |

## 类型定义

### UserConverter.resolveRole（方法修改）

**形态**：`private` 方法
**包路径**：`com.aimedical.modules.commonmodule.auth.converter`

| 当前（L40-41） | 修改后 |
|---------------|--------|
| `return roles.stream()` | `return roles.stream()` |
| `    .min(Comparator.comparingInt(Role::getSort))` | `    .filter(Role::getEnabled)` |
| `    .map(Role::getCode)` | `    .min(Comparator.comparing(Role::getSort, Comparator.nullsLast(Comparator.naturalOrder())))` |
| `    .orElse("");` | `    .map(Role::getCode)` |
| | `    .orElse("");` |

**修改要点**：
- 在 stream 中插入 `.filter(Role::getEnabled)` 过滤已禁用角色（`Role.getEnabled()` 返回 `Boolean`，`Role::getEnabled` 方法引用在 `filter` 中自动拆箱，disabled=false 的 role 被排除）
- 将 `Comparator.comparingInt(Role::getSort)` 改为 `Comparator.comparing(Role::getSort, Comparator.nullsLast(Comparator.naturalOrder()))` 防止 `sort` 为 `null` 时 NPE

### UserConverter.resolvePermissions（方法体修改）

**形态**：`private` 方法
**包路径**：`com.aimedical.modules.commonmodule.auth.converter`

**第一处循环（L63-68，posts→functions）**：
```java
// 当前 L65
if (function != null && function.getCode() != null) {
// 改为
if (function != null && Boolean.TRUE.equals(function.getEnabled()) && function.getCode() != null) {
```

**第二处循环（L75-85，roles→posts→functions）**：
```java
// 当前 L79
if (function != null && function.getCode() != null) {
// 改为
if (function != null && Boolean.TRUE.equals(function.getEnabled()) && function.getCode() != null) {
```

### UserFacadeImpl（类修改）

**形态**：class（implements `UserFacade`）
**包路径**：`com.aimedical.modules.commonmodule.auth`

**字段变更**：
- 新增：`private final UserConverter userConverter;`

**构造器变更**：
```java
// 当前
public UserFacadeImpl(UserRepository userRepository) {

// 改为
public UserFacadeImpl(UserRepository userRepository, UserConverter userConverter) {
    this.userRepository = userRepository;
    this.userConverter = userConverter;
}
```

**toUserInfoResponse 方法变更**：
```java
// 当前（L41-55）
private UserInfoResponse toUserInfoResponse(User user) {
    String role = resolvePrimaryRole(user);
    String position = resolvePosition(user);
    Set<String> permissions = resolvePermissions(user);
    return new UserInfoResponse(/* ... */);
}

// 改为
private UserInfoResponse toUserInfoResponse(User user) {
    return userConverter.toUserInfoResponse(user);
}
```

**删除方法**：
- `resolvePrimaryRole(User user)`（L57-66）
- `resolvePosition(User user)`（L68-76）
- `resolvePermissions(User user)`（L78-105）

**import 变更**：
- 删除：`import com.aimedical.modules.commonmodule.permission.PermissionFunction;`（不再直接使用）
- 删除：`import com.aimedical.modules.commonmodule.permission.Post;`（不再直接使用）
- 删除：`import com.aimedical.modules.commonmodule.permission.Role;`（不再直接使用）
- 删除：`import java.util.Comparator;`（不再直接使用）
- 删除：`import java.util.HashSet;`（不再直接使用）
- 删除：`import java.util.Set;`（不再直接使用）
- 新增：`import com.aimedical.modules.commonmodule.auth.converter.UserConverter;`

**确认**：`PermissionFunction`、`Post`、`Role` 在 `UserFacadeImpl.java` 中仅用于三个私有方法，删除这三个方法后无其他引用，因此这些 import 可安全删除。`Comparator`、`HashSet`、`Set` 同理。

### UserConverterTest 新增测试方法

**形态**：class（package-private test class）
**包路径**：`com.aimedical.modules.commonmodule.auth.converter`

**`shouldFilterDisabledRole`**：
- 创建 `User`，设置一个 `Role`（`enabled=false`, `sort=1`, `code="disabled_role"`）
- 调用 `converter.toUserInfoResponse(user)`
- 断言 `response.role()` 为空字符串

**`shouldHandleNullSort`**：
- 创建 `User`，设置一个 `Role`（`sort=null`, `enabled=true`, `code="doctor"`）
- 调用 `converter.toUserInfoResponse(user)`
- 断言 `response.role()` 为 `"doctor"`（不抛 NPE）

**`shouldFilterDisabledPermission`**：
- 创建 `User`，通过 `Post` 关联一个 `PermissionFunction`（`enabled=false`, `code="disabled:perm"`）
- 调用 `converter.toUserInfoResponse(user)`
- 断言 `response.permissions()` 不包含 `"disabled:perm"`
- 可同时通过 `roles→posts→functions` 路径覆盖，也可仅通过 `posts→functions` 路径

### UserFacadeImplTest 修改

**形态**：class（package-private test class）
**包路径**：`com.aimedical.modules.commonmodule.auth`

**字段变更**：
- 新增：`private final UserConverter userConverter = mock(UserConverter.class);`
- 构造器调用：`new UserFacadeImpl(userRepository)` → `new UserFacadeImpl(userRepository, userConverter)`

**测试方法修改（所有涉及 `toUserInfoResponse` 路径的测试）**：
- 在 `findById_whenUserExists_shouldReturnUserInfo`、`findByUsername_whenUserExists_shouldReturnUserInfo`、`findById_whenUserHasNoRoles_shouldReturnEmptyRole`、`findById_whenAllRolesDisabled_shouldReturnEmptyRole`、`findById_shouldMergePermissionsFromRolesAndPosts` 中，在 mock user 后添加：
  ```java
  UserInfoResponse expectedResponse = new UserInfoResponse(/* ... 与测试断言匹配的值 ... */);
  when(userConverter.toUserInfoResponse(user)).thenReturn(expectedResponse);
  ```
- 也可根据 mock 场景使用通用的 `when(userConverter.toUserInfoResponse(any())).thenReturn(...)` 模式

**import 变更**：
- 新增：`import com.aimedical.modules.commonmodule.auth.converter.UserConverter;`
- 新增：`import static org.mockito.ArgumentMatchers.any;`（如使用 `any()` 匹配器）
- 原有 import 保留

## 错误处理

不涉及新错误类型。`Comparator.nullsLast(Comparator.naturalOrder())` 确保 `sort` 为 `null` 时优先排最后，不抛 NPE。`Boolean.TRUE.equals(function.getEnabled())` 对 `null` 返回 `false`，安全过滤。

## 行为契约

### UserConverter.resolveRole

- **前置**：`roles` stream 元素非 null
- **行为**：过滤 `enabled=false` 的角色；按 `sort` 升序取最小值，`sort=null` 排最后
- **后置**：返回角色 code 或空字符串

### UserConverter.resolvePermissions

- **前置**：`posts`、`functions` 可能为 null
- **行为**：两层循环中均过滤 `enabled=false` 的 `PermissionFunction`
- **后置**：返回权限 code 集合

### UserFacadeImpl.toUserInfoResponse

- **行为变更**：纯委托 `userConverter.toUserInfoResponse(user)`，不再自己实现转换逻辑
- **后置**：返回值与 UserConverter 行为一致

## 依赖关系

| 依赖 | 说明 |
|------|------|
| `UserConverter`（当前类） | 无其他 Bean 依赖，纯 `@Component` |
| `UserFacadeImpl → UserConverter` | 新增构造器注入，不形成循环 |
| `Role.getEnabled()` | 返回 `Boolean`，`filter(Role::getEnabled)` 自动拆箱 |
| `PermissionFunction.getEnabled()` | 返回 `Boolean`，需使用 `Boolean.TRUE.equals(...)` 防御 null |
| `Role.getSort()` | 返回 `Integer`，可为 null，需 `nullsLast` |

## 测试适配

### UserConverterTest（+3 测试）

| 测试方法 | 正向 | 边界 | 断言 |
|---------|------|------|------|
| `shouldFilterDisabledRole` | — | 禁用角色过滤 | `assertEquals("", response.role())` |
| `shouldHandleNullSort` | null sort 不抛 NPE | — | `assertEquals("doctor", response.role())` |
| `shouldFilterDisabledPermission` | — | 禁用权限过滤 | `assertFalse(response.permissions().contains("disabled:perm"))` |

### UserFacadeImplTest（修改）

| 测试方法 | 修改内容 |
|---------|---------|
| `findById_whenUserExists_shouldReturnUserInfo` | 构造器传 `userConverter`；mock `userConverter.toUserInfoResponse(user)` 返回预期响应 |
| `findByUsername_whenUserExists_shouldReturnUserInfo` | 同上 |
| `findById_whenUserNotFound_shouldReturnNull` | 仅改构造器传参，不涉及 `toUserInfoResponse` |
| `findByUsername_whenUserNotFound_shouldReturnNull` | 同上 |
| `existsById_*` | 仅改构造器传参 |
| `findById_nullInput_shouldReturnNull` | 仅改构造器传参 |
| `findByUsername_nullInput_shouldReturnNull` | 仅改构造器传参 |
| `existsById_nullInput_shouldReturnFalse` | 仅改构造器传参 |
| `findById_whenUserHasNoRoles_shouldReturnEmptyRole` | mock `userConverter.toUserInfoResponse(user)` 返回预期响应 |
| `findById_whenAllRolesDisabled_shouldReturnEmptyRole` | mock `userConverter.toUserInfoResponse(user)` 返回预期响应 |
| `findById_shouldMergePermissionsFromRolesAndPosts` | mock `userConverter.toUserInfoResponse(user)` 返回预期响应 |

所有断言不变，仅增加构造器参数和对 `userConverter.toUserInfoResponse` 的 mock 设置。
