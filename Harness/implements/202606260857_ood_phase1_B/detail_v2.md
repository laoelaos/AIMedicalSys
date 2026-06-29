# 详细设计（v2）

## 概述

依据 OOD 文档 `Docs/05_ood_phase1_B.md` 第 12 节 Stage 1（tasks 1.6-1.15, 1.20），完成 Phase 1 包 B 认证/菜单 DTO 的 Java 17 record 改造、新 DTO 补全、GlobalErrorCode 扩展及调用方适配。

核心变更：
- 7 个现有 POJO DTO 改写为 record（`LoginRequest`、`ProfileUpdateRequest`、`MenuCreateRequest`、`LoginResponse`、`UserInfoResponse`、`MenuResponse`）或 PATCH 语义 POJO（`MenuUpdateRequest`）
- 3 个新 DTO 文件（`RefreshTokenRequest`、`TokenRefreshResponse`、`PasswordChangeRequest`）
- `GlobalErrorCode` 枚举新增 14 个值
- 调用方（`AuthService`/`AuthServiceImpl`/`AuthController`/`MenuServiceImpl`/`MenuController`）因 record 访问方式变更和字段名变更同步适配

## 文件规划

| 文件路径（相对 `AIMedical/backend/`） | 操作 | 职责 |
|---------|------|------|
| `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/LoginRequest.java` | 修改 | POJO→record，字段缩减 |
| `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/ProfileUpdateRequest.java` | 修改 | POJO→record，补 `@NotBlank` + 手机号正则 |
| `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/MenuCreateRequest.java` | 修改 | POJO→record，字段按 OOD 5.2 重定义 |
| `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/MenuUpdateRequest.java` | 修改 | POJO PATCH 语义，`@JsonInclude(NON_NULL)`，移除 `MenuType` 依赖 |
| `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/RefreshTokenRequest.java` | 新增 | 新 record |
| `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/PasswordChangeRequest.java` | 新增 | 新 record |
| `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/response/LoginResponse.java` | 修改 | POJO→record + Breaking Change |
| `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/response/UserInfoResponse.java` | 修改 | POJO→record + Breaking Change |
| `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/response/TokenRefreshResponse.java` | 新增 | 新 record |
| `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/response/MenuResponse.java` | 修改 | POJO→record，字段按 OOD 5.2 重定义 |
| `common/common/src/main/java/com/aimedical/common/exception/GlobalErrorCode.java` | 修改 | 枚举新增 14 个值 |
| `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/AuthService.java` | 修改 | `refreshToken` 返回类型 `LoginResponse`→`TokenRefreshResponse` |
| `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/AuthServiceImpl.java` | 修改 | DTO 构造/访问适配 + refresh 返回类型变更 |
| `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/controller/AuthController.java` | 修改 | refresh 端点返回 `TokenRefreshResponse`，新增 `password`/`refresh` 端点参数 |
| `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/MenuServiceImpl.java` | 修改 | MenuCreateRequest/MenuResponse 访问适配 |
| `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/controller/MenuController.java` | 修改 | MenuResponse 访问适配（`getSortOrder()`→`sort()` 等） |

## 类型定义

### LoginRequest (record)

**形态**：Java 17 record
**包路径**：`com.aimedical.modules.commonmodule.dto.request.LoginRequest`
**职责**：登录请求体；字段较原 POJO 精简，移除 `@Size(min=3, max=50)` 用户名约束，密码约束改为 `@Size(min=1, max=64)`

```java
public record LoginRequest(
    @NotBlank String username,
    @NotBlank @Size(min = 1, max = 64) String password
) {}
```

**公开接口**：`username()`、`password()`（record 自动生成）
**构造方式**：`new LoginRequest("username", "password")`
**类型关系**：替换原 POJO；当前无 `@Getter`/`@Setter` 依赖

**调用方适配**：
- `AuthServiceImpl.login()`：`request.getUsername()` → `request.username()`；`request.getPassword()` → `request.password()`
- `AuthController.login()`：使用 `@Valid @RequestBody LoginRequest` 不变

---

### ProfileUpdateRequest (record)

**形态**：Java 17 record
**包路径**：`com.aimedical.modules.commonmodule.dto.request.ProfileUpdateRequest`
**职责**：个人资料更新请求；新增 `@NotBlank` on nickname，手机号加正则校验

```java
public record ProfileUpdateRequest(
    @NotBlank(message = "昵称不能为空") @Size(max = 50) String nickname,
    @Pattern(regexp = "^1[3-9]\\d{9}$") String phone,
    @Email @Size(max = 100) String email
) {}
```

**公开接口**：`nickname()`、`phone()`、`email()`
**构造方式**：`new ProfileUpdateRequest("昵称", "13800138000", "email@example.com")`
**类型关系**：替换原 POJO

**调用方适配**：
- `AuthServiceImpl.updateProfile()`：`request.getNickname()` → `request.nickname()`
- `AuthController.updateMe()`：使用 `@Valid @RequestBody ProfileUpdateRequest` 不变

---

### MenuCreateRequest (record)

**形态**：Java 17 record
**包路径**：`com.aimedical.modules.commonmodule.dto.request.MenuCreateRequest`
**职责**：菜单创建请求；字段较原 POJO 大幅精简：移除 `code`/`description`/`type`(MenuType)/`sortOrder`/`enabled`，新增 `permission`/`component`/`sort`

```java
public record MenuCreateRequest(
    @NotBlank String name,
    @NotBlank String permission,
    Long parentId,
    String path,
    String component,
    String icon,
    Integer sort,
    @NotNull Boolean visible
) {}
```

**公开接口**：`name()`、`permission()`、`parentId()`、`path()`、`component()`、`icon()`、`sort()`、`visible()`
**构造方式**：`new MenuCreateRequest("name", "permission", null, "/path", "Component", "icon", 0, true)`
**类型关系**：替换原 POJO；不再依赖 `com.aimedical.common.base.MenuType`

**调用方适配**（`MenuServiceImpl`）：
| 原代码 | 新代码 |
|--------|--------|
| `request.getCode()` | `request.permission()` |
| `request.getName()` | `request.name()` |
| `request.getDescription()` | 移除（字段不存在） |
| `request.getParentId()` | `request.parentId()` |
| `request.getPath()` | `request.path()` |
| `request.getIcon()` | `request.icon()` |
| `request.getType().getCode()` | 移除（MenuType 不再存在于 DTO） |
| `request.getSortOrder()` | `request.sort()` |
| `request.getVisible()` | `request.visible()` |
| `request.getEnabled()` | 移除（字段不存在） |

---

### MenuUpdateRequest (PATCH POJO)

**形态**：传统 POJO（非 record）
**包路径**：`com.aimedical.modules.commonmodule.dto.request.MenuUpdateRequest`
**职责**：菜单局部更新；PATCH 语义，仅提供需变更的字段；`@JsonInclude(JsonInclude.Include.NON_NULL)` 控制序列化，不验证 `@Valid` 全量约束

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MenuUpdateRequest {
    private Long id;
    private String name;
    private String permission;
    private Long parentId;
    private String path;
    private String component;
    private String icon;
    private Integer sort;
    private Boolean visible;
    // getter/setter 无特殊约束
}
```

**公开接口**：全部 getter/setter（`getId/setId`、`getName/setName`、`getPermission/setPermission`、`getParentId/setParentId`、`getPath/setPath`、`getComponent/setComponent`、`getIcon/setIcon`、`getSort/setSort`、`getVisible/setVisible`）
**构造方式**：`new MenuUpdateRequest()` + setter
**类型关系**：替换原 POJO；不再依赖 `com.aimedical.common.base.MenuType`

**调用方适配**（`MenuServiceImpl`）：
| 原代码 | 新代码 |
|--------|--------|
| `request.getCode()` | `request.getPermission()` |
| `request.getSortOrder()` | `request.getSort()` |
| `request.getType()` | 移除 |

---

### RefreshTokenRequest (record)

**形态**：Java 17 record
**包路径**：`com.aimedical.modules.commonmodule.dto.request.RefreshTokenRequest`
**职责**：Token 刷新请求体 / 登出可选请求体

```java
public record RefreshTokenRequest(
    @NotBlank String refreshToken
) {}
```

**公开接口**：`refreshToken()`
**构造方式**：`new RefreshTokenRequest("eyJhbGciOiJIUzI1NiJ9...")`
**类型关系**：无已有替换关系

**使用场景**：
- `POST /api/auth/refresh`：`@RequestBody @Valid RefreshTokenRequest`
- `POST /api/auth/logout`：`@RequestBody(required=false) RefreshTokenRequest`

---

### PasswordChangeRequest (record)

**形态**：Java 17 record
**包路径**：`com.aimedical.modules.commonmodule.dto.request.PasswordChangeRequest`
**职责**：密码修改请求体

```java
public record PasswordChangeRequest(
    @NotBlank @Size(max = 128) String oldPassword,
    @NotBlank @Size(min = 8, max = 64) String newPassword
) {}
```

**公开接口**：`oldPassword()`、`newPassword()`
**构造方式**：`new PasswordChangeRequest("oldPass", "newPass")`
**类型关系**：无已有替换关系，新增端点 `PUT /api/auth/password`

---

### LoginResponse (record)

**形态**：Java 17 record
**包路径**：`com.aimedical.modules.commonmodule.dto.response.LoginResponse`
**职责**：登录成功响应体；Breaking Change：`token`→`accessToken`，新增 `userId`/`username`/`refreshToken`/`passwordChangeRequired`，`expiresIn` 由 `Long`→`long`

```java
public record LoginResponse(
    Long userId,
    String username,
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    boolean passwordChangeRequired,
    UserInfoResponse user
) {}
```

**公开接口**：`userId()`、`username()`、`accessToken()`、`refreshToken()`、`tokenType()`、`expiresIn()`、`passwordChangeRequired()`、`user()`
**构造方式**：`new LoginResponse(userId, username, accessToken, refreshToken, "Bearer", 900L, false, userInfo)`
**类型关系**：替换原 POJO

**调用方适配**（`AuthServiceImpl`）：
| 原代码 | 新代码 |
|--------|--------|
| `new LoginResponse()` | `new LoginResponse(..., ..., ..., ..., ..., ..., ..., ...)` |
| `response.setToken(token)` | 构造器传参 |
| `response.setTokenType(...)` | 构造器传参 |
| `response.setExpiresIn(...)` | 构造器传参 |
| `response.setUser(...)` | 构造器传参 |
| `response.getToken()`（不存在于当前 impl） | `response.accessToken()` |

**调用方适配**（`AuthService` 接口）：
- `login()` 返回 `LoginResponse`：不变（类型签名同）
- `refreshToken()` 返回 `LoginResponse` → `TokenRefreshResponse`：见 `TokenRefreshResponse` 定义

---

### UserInfoResponse (record)

**形态**：Java 17 record
**包路径**：`com.aimedical.modules.commonmodule.dto.response.UserInfoResponse`
**职责**：用户详细信息响应体；Breaking Change：`realName` 已有（不来自 `nickname`），`role` 已有（不来自 `userType`）；新增 `phone`/`email`/`position`/`permissions`（`Set<String>` 替代 `List<String>`）

```java
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

**公开接口**：`id()`、`username()`、`realName()`、`phone()`、`email()`、`role()`、`position()`、`permissions()`
**构造方式**：`new UserInfoResponse(id, username, realName, phone, email, role, position, permissions)`
**类型关系**：替换原 POJO；`permissions` 类型 `List<String>` → `Set<String>`

**调用方适配**（`AuthServiceImpl.buildUserInfoResponse`）：
| 原代码 | 新代码 |
|--------|--------|
| `new UserInfoResponse()` | `new UserInfoResponse(..., ..., ..., ..., ..., ..., ..., ...)` |
| `response.setId(...)` | 构造器传参 |
| `response.setUsername(...)` | 构造器传参 |
| `response.setRealName(...)` | 构造器传参 |
| `response.setRole(...)` | 构造器传参 |
| `response.setPosition(...)` | 构造器传参 |
| `response.setPermissions(...)` | 构造器传参，`Set<String>` 替代 `List<String>` |
| `response.getPermissions()`（不存在于当前 impl） | 无变更 |

---

### TokenRefreshResponse (record)

**形态**：Java 17 record
**包路径**：`com.aimedical.modules.commonmodule.dto.response.TokenRefreshResponse`
**职责**：Token 刷新响应体；不含 user 字段（前端需调用 `GET /api/auth/me` 获取最新用户信息）

```java
public record TokenRefreshResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn
) {}
```

**公开接口**：`accessToken()`、`refreshToken()`、`tokenType()`、`expiresIn()`
**构造方式**：`new TokenRefreshResponse(accessToken, refreshToken, "Bearer", 900L)`
**类型关系**：无已有替换关系

**使用场景**：
- `AuthService.refreshToken()` 返回类型由 `LoginResponse` → `TokenRefreshResponse`
- `AuthController.refresh()` 返回类型由 `Result<LoginResponse>` → `Result<TokenRefreshResponse>`

---

### MenuResponse (record)

**形态**：Java 17 record
**包路径**：`com.aimedical.modules.commonmodule.dto.response.MenuResponse`
**职责**：菜单树节点响应；字段精简：移除 `parentId`/`type`/`visible`/`enabled`/`sortOrder`，新增 `component`/`permission`/`sort`

```java
public record MenuResponse(
    Long id,
    String name,
    String path,
    String component,
    String icon,
    String permission,
    Integer sort,
    List<MenuResponse> children
) {}
```

**公开接口**：`id()`、`name()`、`path()`、`component()`、`icon()`、`permission()`、`sort()`、`children()`
**构造方式**：`new MenuResponse(id, name, path, component, icon, permission, sort, children)`
**类型关系**：替换原 POJO（原 11 字段 → 8 字段）

**调用方适配**（`MenuServiceImpl.convertToMenuResponse`）：
| 原代码 | 新代码 |
|--------|--------|
| `new MenuResponse()` | `new MenuResponse(id, name, path, component, icon, permission, sort, children)` |
| `response.setId(...)` | 构造器传参 |
| `response.setName(...)` | 构造器传参 |
| `response.setPath(...)` | 构造器传参 |
| `response.setIcon(...)` | 构造器传参 |
| `response.setPermission(function.getCode())` | `function.getCode()`→`function.getCode()`（实体侧不变） |
| `response.setSortOrder(function.getSortOrder())` | 构造器传参，字段名 `sortOrder`→`sort` |
| `response.setParentId(...)` | 移除（字段不存在） |
| `response.setType(...)` | 移除（字段不存在） |
| `response.setVisible(...)` | 移除（字段不存在） |
| `response.setEnabled(...)` | 移除（字段不存在） |
| `response.setChildren(...)` | 构造器中传入 |
| `menu.getSortOrder()`（buildMenuTree 中比较器） | `menu.sort()` |
| `menu.getParentId()`（buildMenuTree 中逻辑） | 移除（ParentId 字段不存在，改用其他方式：保留菜单实体侧 parentId，从 DB 查询时以 `Function.parent` 关联确定树结构，DTO 层不再暴露 parentId） |
| `menu.getId()`（buildMenuTree 中映射） | `menu.id()` |
| `menu.getChildren()`（buildMenuTree 中挂载） | `menu.children()` |
| `parent.getChildren() == null` | `parent.children() == null` |
| `parent.setChildren(...)` | 无法调用 setter（record 不可变）；重构 `buildMenuTree` 为可变列表+不可变 record 的模式 |

**`buildMenuTree` 重构说明**：
MenuResponse 变为 record 后不可变，无法在 `buildMenuTree` 中通过 setter 挂载 children。需改为两阶段构建：
1. 先用 `new MenuResponse(..., null)` 创建不含 children 的节点存入 Map
2. 遍历时通过 children 列表的引用挂载子节点
3. 或用逐层构建的策略：先构建叶子节点，再向上逐层创建父节点
**推荐方案**：第一阶段 `convertToMenuResponse` 返回不含 children 的 MenuResponse（children=null）；第二阶段 `buildMenuTree` 使用 `Map<Long, MenuResponse>` 缓存，然后从叶子向上逐层用含 children 的构造函数重建父节点。因 `MenuResponse` 未提供 `withChildren()` 方法，需重建父节点实例。

替代方案：在 `MenuResponse` 中辅助方法 `public MenuResponse withChildren(List<MenuResponse> children) { return new MenuResponse(id, name, path, component, icon, permission, sort, children); }`。此模式在 Java 17 record 中不推荐（record 应保持纯数据），但与已有的 `children` 字段职责一致。

**设计决策**：在 `MenuResponse` 中添加 `withChildren` 实例方法（类似 Scala case class copy），用于树构建时生成含 children 的新实例。属于 record 中允许的实例方法，不破坏 record 的不可变性契约。

```java
public MenuResponse withChildren(List<MenuResponse> children) {
    return new MenuResponse(this.id, this.name, this.path, this.component,
            this.icon, this.permission, this.sort, children);
}
```

---

### GlobalErrorCode 枚举扩展

**形态**：Java enum（implements ErrorCode）
**包路径**：`com.aimedical.common.exception.GlobalErrorCode`
**职责**：全局错误码枚举，在已有 6 个值基础上新增 14 个认证/业务错误码

```java
public enum GlobalErrorCode implements ErrorCode {
    SUCCESS("SUCCESS", "成功"),
    SYSTEM_ERROR("SYSTEM_ERROR", "系统异常"),
    PARAM_INVALID("PARAM_INVALID", "参数校验失败"),
    NOT_FOUND("NOT_FOUND", "资源不存在"),
    UNAUTHORIZED("UNAUTHORIZED", "未认证"),
    FORBIDDEN("FORBIDDEN", "无权限"),
    // 新增（14个）：
    LOGIN_FAILED("LOGIN_FAILED", "用户名或密码错误"),
    ACCOUNT_DISABLED("ACCOUNT_DISABLED", "账户已被管理员停用"),
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", "账户已锁定，请{锁定时间}后重试"),
    RATE_LIMITED("RATE_LIMITED", "登录尝试过于频繁，请稍后重试"),
    RATE_LIMITED_GLOBAL("RATE_LIMITED_GLOBAL", "请求过于频繁，请稍后重试"),
    PASSWORD_TOO_SHORT("PASSWORD_TOO_SHORT", "密码长度不能少于8位"),
    PASSWORD_TOO_LONG("PASSWORD_TOO_LONG", "密码长度不能超过64位"),
    PASSWORD_WEAK("PASSWORD_WEAK", "密码不符合复杂度要求"),
    PASSWORD_CONTAINS_USERNAME("PASSWORD_CONTAINS_USERNAME", "密码不能包含用户名"),
    PASSWORD_COMMON("PASSWORD_COMMON", "密码过于常见"),
    TOKEN_REFRESH_FAILED("TOKEN_REFRESH_FAILED", "令牌刷新失败，请重新登录"),
    PASSWORD_CHANGE_REQUIRED("PASSWORD_CHANGE_REQUIRED", "需要修改密码"),
    CHILDREN_EXIST("CHILDREN_EXIST", "存在子菜单，无法删除"),
    PASSWORD_MISMATCH("PASSWORD_MISMATCH", "旧密码不正确");
    // ... 已有 code/message 字段、构造器、getter
}
```

**公开接口**：`getCode()`、`getMessage()`（继承自 `ErrorCode` 接口）
**构造方式**：枚举构造器 `GlobalErrorCode(String code, String message)`（已有）
**类型关系**：已有 `implements ErrorCode` 不变

---

### AuthService 接口变更

**形态**：Java interface
**包路径**：`com.aimedical.modules.commonmodule.service.AuthService`
**职责**：统一认证服务契约

**方法签名变更**：
```java
public interface AuthService {
    LoginResponse login(LoginRequest request);             // 不变（类型同）
    void logout(String token);                              // 不变
    TokenRefreshResponse refreshToken(String token);        // 返回类型 LoginResponse → TokenRefreshResponse
    UserInfoResponse getCurrentUser(String token);         // 不变（UserInfoResponse 类型同）
    UserInfoResponse updateProfile(String token, ProfileUpdateRequest request); // 不变
}
```

**类型关系**：新增 `import com.aimedical.modules.commonmodule.dto.response.TokenRefreshResponse`

---

### AuthServiceImpl 变更

**构造方式变更**（所有 DTO 实例化处）：

#### `login()` 方法
```java
// 原：
LoginResponse response = new LoginResponse();
response.setToken(token);
response.setTokenType(jwtUtil.getTokenType());
response.setExpiresIn(jwtUtil.getExpirationTime());
response.setUser(buildUserInfoResponse(user));

// 新：
LoginResponse response = new LoginResponse(
    user.getId(),
    user.getUsername(),
    token,
    null,  // refreshToken — Phase 1 暂为 null，Phase 2 JwtTokenProvider 填充
    jwtUtil.getTokenType(),
    jwtUtil.getExpirationTime(),
    false, // passwordChangeRequired — Phase 2 PasswordChangeService 判定
    buildUserInfoResponse(user)
);
```

#### `refreshToken()` 方法
```java
// 返回类型 LoginResponse → TokenRefreshResponse
// 原：
LoginResponse response = new LoginResponse();
response.setToken(newToken);
response.setTokenType(jwtUtil.getTokenType());
response.setExpiresIn(jwtUtil.getExpirationTime());
response.setUser(buildUserInfoResponse(user));

// 新：
TokenRefreshResponse response = new TokenRefreshResponse(
    newToken,
    null,  // refreshToken — Phase 1 暂为 null
    jwtUtil.getTokenType(),
    jwtUtil.getExpirationTime()
);
```

#### `buildUserInfoResponse()` 方法
```java
// 原：
UserInfoResponse response = new UserInfoResponse();
response.setId(user.getId());
response.setUsername(user.getUsername());
response.setRealName(user.getNickname());
response.setRole(user.getUserType().getCode());
// ... position, permissions setters ...

// 新：
Set<String> permissions = new HashSet<>(/* ... */);
UserInfoResponse response = new UserInfoResponse(
    user.getId(),
    user.getUsername(),
    user.getNickname(),      // User.nickname → realName（UserConverter 映射，Phase 3）
    user.getPhone(),
    user.getEmail(),
    user.getUserType().getCode(),  // User.userType → role
    position,
    permissions
);
```

#### `updateProfile()` 方法
```java
// 原：
if (request.getNickname() != null) {
    user.setNickname(request.getNickname());
}

// 新：
if (request.nickname() != null) {
    user.setNickname(request.nickname());
}
// phone, email 同理
```

---

### AuthController 变更

**刷新端点**：
```java
// 原：
@PostMapping("/refresh")
public Result<LoginResponse> refresh(@RequestHeader("Authorization") String authHeader) { ... }

// 新：
@PostMapping("/refresh")
public Result<TokenRefreshResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) { ... }
```
变更说明：刷新端点改为从请求体接收 `RefreshTokenRequest`（而非从 Header 取 Access Token），返回 `TokenRefreshResponse`（不含 user）。

**登出端点**：
```java
// 原：
@PostMapping("/logout")
public Result<Void> logout(@RequestHeader("Authorization") String authHeader) { ... }

// 新：
@PostMapping("/logout")
public Result<Void> logout(
    @RequestHeader("Authorization") String authHeader,
    @RequestBody(required = false) RefreshTokenRequest refreshTokenRequest) { ... }
```

**新增密码修改端点**：
```java
@PutMapping("/password")
public Result<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) { ... }
```

**新增导入**：
```java
import com.aimedical.modules.commonmodule.dto.request.PasswordChangeRequest;
import com.aimedical.modules.commonmodule.dto.request.RefreshTokenRequest;
import com.aimedical.modules.commonmodule.dto.response.TokenRefreshResponse;
```

---

### MenuServiceImpl 变更

#### `convertToMenuResponse()`
```java
// 原：
MenuResponse response = new MenuResponse();
response.setId(function.getId());
response.setName(function.getName());
response.setPath(function.getPath());
response.setIcon(function.getIcon());
response.setPermission(function.getCode());
response.setSortOrder(function.getSortOrder());
response.setParentId(function.getParent() != null ? function.getParent().getId() : null);
response.setType(function.getType());
response.setVisible(function.getVisible());
response.setEnabled(function.getEnabled());

// 新：
MenuResponse response = new MenuResponse(
    function.getId(),
    function.getName(),
    function.getPath(),
    null,  // component — 当前 Function 实体无此字段，暂为 null
    function.getIcon(),
    function.getCode(),   // Function.code → permission
    function.getSortOrder(),
    null   // children — 由 buildMenuTree 填充
);
```

#### `buildMenuTree()` 重构
```java
// 原：基于 MenuResponse setter 可变性
// 新：基于 MenuResponse.withChildren() 不可变构建

// 第一阶段：按原逻辑装入 Map<Long, MenuResponse>
Map<Long, MenuResponse> idToMenu = new LinkedHashMap<>();
for (MenuResponse menu : menus) {
    idToMenu.put(menu.id(), menu);
}

// 第二阶段：按 parentId 挂载，用 withChildren 重建父节点
List<MenuResponse> roots = new ArrayList<>();
for (MenuResponse menu : menus) {
    Long parentId = getParentId(menu);  // 从外部传入的 entity 映射表获取
    if (parentId == null) {
        roots.add(menu);
    } else {
        MenuResponse parent = idToMenu.get(parentId);
        if (parent != null) {
            List<MenuResponse> newChildren = new ArrayList<>();
            if (parent.children() != null) {
                newChildren.addAll(parent.children());
            }
            newChildren.add(menu);
            idToMenu.put(parent.id(), parent.withChildren(newChildren));
        } else {
            roots.add(menu);
        }
    }
}

// 注意：以上重构依赖持有 Function.parentId 的外部映射
// 更简洁的方案：convertToMenuResponse 时不再丢掉 parentId，而是将 parentId 通过扩展信息传递
```

**注意**：上述 `buildMenuTree` 重构方案需额外持有 `Function.parentId` → `MenuResponse` 的映射关系。推荐在 `convertToMenuResponse` 阶段同时记录 `Map<Long, Long> menuParentIdMap`（menuId → parentId），供 `buildMenuTree` 读取。

替代方案：保持 `MenuServiceImpl` 中持有 `Function` 实体的 parentId 映射（在 `getUserMenuTree`/`getAllMenus` 中构建 map），简化 `buildMenuTree` 逻辑。

#### 排序比较器
```java
// 原：
.sorted(Comparator.comparing(MenuResponse::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))

// 新：
.sorted(Comparator.comparing(MenuResponse::sort, Comparator.nullsLast(Comparator.naturalOrder())))
```

#### `createMenu()` 方法
```java
// 原：
if (functionRepository.existsByCode(request.getCode())) { ... }
function.setCode(request.getCode());
function.setDescription(request.getDescription());
function.setType(request.getType().getCode());
function.setPath(request.getPath());
function.setIcon(request.getIcon());
function.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
function.setVisible(request.getVisible() != null ? request.getVisible() : true);
function.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);

// 新：
if (functionRepository.existsByCode(request.permission())) { ... }
function.setCode(request.permission());
function.setPath(request.path());
function.setIcon(request.icon());
function.setSortOrder(request.sort() != null ? request.sort() : 0);
function.setVisible(request.visible() != null ? request.visible() : true);
```

#### `updateMenu()` 方法
```java
// 原：
if (request.getCode() != null) { ... function.setCode(request.getCode()); }
if (request.getDescription() != null) { ... }
if (request.getSortOrder() != null) { function.setSortOrder(request.getSortOrder()); }
if (request.getType() != null) { function.setType(request.getType().getCode()); }
if (request.getVisible() != null) { function.setVisible(request.getVisible()); }
if (request.getEnabled() != null) { function.setEnabled(request.getEnabled()); }

// 新：
if (request.getPermission() != null) { ... function.setCode(request.getPermission()); }
if (request.getSort() != null) { function.setSortOrder(request.getSort()); }
if (request.getVisible() != null) { function.setVisible(request.getVisible()); }
// getDescription(), getType(), getEnabled() 移除
```

---

### MenuController 变更

**无 getter/setter 适配**：`MenuController` 不直接访问 `MenuResponse` 字段（仅通过 `MenuService` 获取并返回），因此无 Breaking Change 适配。保持现有代码不变。

**仅 import 更新**（`MenuResponse` package 不变，无需修改）。

---

## 错误处理

- **GlobalErrorCode 枚举扩展**：新增 14 个枚举值，遵循已有 `(String code, String message)` 构造器模式；新增字段仅在新增代码中使用，现有代码（`SUCCESS`/`SYSTEM_ERROR`/`PARAM_INVALID`/`NOT_FOUND`/`UNAUTHORIZED`/`FORBIDDEN`）引用不受影响。
- **ACCOUNT_LOCKED message**：含占位符 `{锁定时间}`，运行时由 `LoginAttemptTracker`（Phase 2）根据锁定维度动态格式化。仅作为默认 message，运行时覆盖。
- **调用方异常处理不变**：所有 `BusinessException` 抛出的错误码引用仍以 `GlobalErrorCode.XXX` 形式，变更仅发生在新增错误码引用处。
- **record 序列化**：Java 17 record 的 JSON 序列化由 Jackson 的 `@JsonAutoDetect` 或 `@JsonProperty` 自动处理（Spring Boot 默认配置支持 record），字段名即为 record component 名。无需额外注解。

## 行为契约

### LoginRequest
- **前置条件**：`username` 非空；`password` 非空且 1-64 字符。
- **后置条件**：无副作用。

### ProfileUpdateRequest
- **前置条件**：`nickname` 非空且 ≤50 字符；`phone` 非空则匹配 `^1[3-9]\d{9}$`；`email` 非空则合法邮箱格式且 ≤100 字符。
- **后置条件**：无副作用。

### MenuCreateRequest
- **前置条件**：`name` 非空；`permission` 非空；`visible` 非空。
- **后置条件**：无副作用。

### MenuUpdateRequest
- **前置条件**：至少一个字段被设置（非 null），否则视为无操作。
- **后置条件**：无副作用。
- **不变量**：`id` 字段可选；若携带则与路径参数 `{id}` 一致校验由 Controller 层处理（本任务不实现此校验逻辑，仅在 DTO 层保留 `id` 字段）。

### LoginResponse
- **后置条件**：`expiresIn` 始终为 access token 固定有效期（秒），从签发时刻起固定不变；`passwordChangeRequired` 为 `false`（Phase 1 默认值，Phase 2 由 `PasswordChangeService` 动态设置）；`user` 始终非 null。

### UserInfoResponse
- **后置条件**：`permissions` 为 `Set<String>` 而非 `List<String>`，保证权限码去重；`role` 为当前用户主角色 code；`position` 为当前岗位 code（可为 null）。

### TokenRefreshResponse
- **后置条件**：不含 `user` 字段，前端需调用 `GET /api/auth/me` 获取最新用户信息。

### MenuResponse
- **后置条件**：`children` 为 null（叶子节点）或非空列表（父节点）；递归深度由后端菜单数据决定，无深度限制；`permission` 对应后端 Function.code。

### GlobalErrorCode
- **不变量**：新增枚举值的 `code` 字段与枚举名一致（`LOGIN_FAILED.code == "LOGIN_FAILED"`），`message` 字段为固定中文描述；`ACCOUNT_LOCKED.message` 含运行时占位符。

### AuthService.refreshToken
- **返回类型**：`LoginResponse` → `TokenRefreshResponse`（breaking change）。
- **前置条件**：调用方需传 Refresh Token 字符串（非 Access Token）。
- **后置条件**：返回新签发的 accessToken + refreshToken + tokenType + expiresIn，不返回用户信息。

### MenuServiceImpl.buildMenuTree
- **前置条件**：MenuResponse 为不可变 record，需通过 `withChildren` 方法构建树。
- **不变量**：`withChildren` 返回新实例，原实例不变（record 不可变性）。
- **调用顺序**：先 `convertToMenuResponse`（children=null）→ `buildMenuTree`（用 withChildren 挂载 children）。

## 依赖关系

### 已有依赖（不变）
- `com.aimedical.common.exception.ErrorCode`：`GlobalErrorCode` 实现此接口。
- `com.aimedical.common.exception.BusinessException`：`AuthServiceImpl` 抛出。
- `com.aimedical.common.base.MenuType`：`MenuCreateRequest` 和 `MenuUpdateRequest` **不再依赖**此类型。
- `com.aimedical.modules.commonmodule.permission.Function` / `User` / `Post` / `UserRepository` / `FunctionRepository`：`MenuServiceImpl` 和 `AuthServiceImpl` 的内部依赖。
- `com.aimedical.modules.commonmodule.jwt.JwtUtil`：`AuthServiceImpl` 和 `AuthController` 依赖。
- `com.aimedical.modules.commonmodule.api.UserType`：`AuthServiceImpl` 引用。
- `jakarta.validation.constraints.*`：record DTO 的校验注解。

### 新增依赖
- `com.aimedical.modules.commonmodule.dto.request.PasswordChangeRequest`：新增，`AuthController` 引用。
- `com.aimedical.modules.commonmodule.dto.request.RefreshTokenRequest`：新增，`AuthController` 引用。
- `com.aimedical.modules.commonmodule.dto.response.TokenRefreshResponse`：新增，`AuthService` / `AuthServiceImpl` / `AuthController` 引用。

### 移除依赖
- `com.aimedical.common.base.MenuType`：从 `MenuCreateRequest` 和 `MenuUpdateRequest` 中移除。

### 暴露给后续任务的公开接口
- `GlobalErrorCode.LOGIN_FAILED` / `ACCOUNT_DISABLED` / `ACCOUNT_LOCKED` / `RATE_LIMITED` / `RATE_LIMITED_GLOBAL` / `PASSWORD_*` / `TOKEN_REFRESH_FAILED` / `PASSWORD_CHANGE_REQUIRED` / `CHILDREN_EXIST` / `PASSWORD_MISMATCH`：Phase 2 `AuthServiceImpl`、`JwtAuthenticationFilter`、`LoginAttemptTracker`、`PasswordPolicy`、`MenuServiceImpl.deleteMenu` 等引用。
- `LoginResponse` record（含 `accessToken`、`refreshToken`、`passwordChangeRequired`）：Phase 2 `AuthServiceImpl.login()` 构造和 `AuthController` 返回。
- `TokenRefreshResponse` record：Phase 2 `AuthServiceImpl.refreshToken()` 返回。
- `UserInfoResponse` record（含 `realName`、`role`、`position`、`permissions`）：Phase 2 `AuthServiceImpl.getCurrentUser()` 返回，Phase 3 `UserConverter` 转换目标。
- `MenuResponse` record（含 `withChildren` 方法）：Phase 2 `MenuServiceImpl.convertToMenuResponse` 和 `buildMenuTree` 使用。
- `ProfileUpdateRequest` record（含 `@Pattern` 手机号校验）：Phase 2 `AuthServiceImpl.updateProfile()` 参数。
- `PasswordChangeRequest` record：Phase 2 `AuthServiceImpl.changePassword()` 参数。

### 不在此范围
- `Function`→`PermissionFunction` 重命名（M8，独立任务）
- `JwtUtil`→`JwtTokenProvider` 改造（Phase 3 任务）
- `schema.sql` DDL 变更（独立任务）
- 集成测试用例新增（阶段 4 统一处理）
- `AuthServiceImpl.login()` 中 `LoginAttemptTracker` / `InMemoryRateLimitGuard` 集成（Phase 2 任务）
- `AuthController` 中 `CurrentUser` 接口替代 JwtUtil（Phase 3 任务）
- `MenuUpdateRequest` 中 `id` 字段与路径参数一致性的 Controller 层校验逻辑
