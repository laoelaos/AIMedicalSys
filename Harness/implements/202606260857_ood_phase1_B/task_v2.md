# 任务指令（v2）

## 动作
NEW

## 任务描述
将 Phase 1 包 B OOD 第 12 节 Stage 1 中的认证/菜单 DTO 全部改为 Java 17 record（tasks 1.6-1.14），补全新 DTO（RefreshTokenRequest、TokenRefreshResponse、PasswordChangeRequest），扩展 GlobalErrorCode 枚举（1.15），同步修复调用方（1.20）。

预期文件路径（相对于 `C:/Develop/Software/AIMedicalSys/AIMedical/backend/`）：

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| 修改 | `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/LoginRequest.java` | POJO→record：`@NotBlank String username, @NotBlank @Size(min=1, max=64) String password` |
| 修改 | `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/ProfileUpdateRequest.java` | POJO→record：`@NotBlank @Size(max=50) String nickname, @Pattern(regexp="^1[3-9]\\d{9}$") String phone, @Email @Size(max=100) String email` |
| 修改 | `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/MenuCreateRequest.java` | POJO→record：`@NotBlank String name, @NotBlank String permission, Long parentId, String path, String component, String icon, Integer sort, @NotNull Boolean visible` |
| 修改 | `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/MenuUpdateRequest.java` | 改为 PATCH 语义 POJO：`@JsonInclude(Include.NON_NULL)`，字段含 `Long id, String name, String permission, Long parentId, String path, String component, String icon, Integer sort, Boolean visible`，无 getter/setter 约束（传统 POJO） |
| 修改 | `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/response/LoginResponse.java` | POJO→record：`Long userId, String username, String accessToken, String refreshToken, String tokenType, long expiresIn, boolean passwordChangeRequired, UserInfoResponse user`。Breaking Change：`token`→`accessToken`，新增 `userId`/`username`/`refreshToken`/`passwordChangeRequired`，`expiresIn` 由 `Long`→`long` |
| 修改 | `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/response/UserInfoResponse.java` | POJO→record：`Long id, String username, String realName, String phone, String email, String role, String position, Set<String> permissions`。Breaking Change：`nickname`→`realName`，`userType`→`role`，新增 `position`/`permissions` |
| 修改 | `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/response/MenuResponse.java` | POJO→record：`Long id, String name, String path, String component, String icon, String permission, Integer sort, List<MenuResponse> children` |
| 新增 | `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/RefreshTokenRequest.java` | 新 record：`@NotBlank String refreshToken` |
| 新增 | `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/request/PasswordChangeRequest.java` | 新 record：`@NotBlank @Size(max=128) String oldPassword, @NotBlank @Size(min=8, max=64) String newPassword` |
| 新增 | `common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/response/TokenRefreshResponse.java` | 新 record：`String accessToken, String refreshToken, String tokenType, long expiresIn` |
| 修改 | `common/common/src/main/java/com/aimedical/common/exception/GlobalErrorCode.java` | 扩展枚举新增 14 个值（OOD 10.2 节完整枚举） |
| 修改 | 各调用方文件 | 因 POJO→record Breaking Change 同步适配 `getXxx()` → `xxx()` 访问方式 |

## 选择理由
DTO 是认证流程中所有 Service / Controller / Filter 的输入输出契约。R1 已完成实体层稳定（User/Role/Post 字段扩展），DTO 层作为下一层契约必须在 Service 和 Controller 改造前对齐。OOD 5.2 节定义了完整的 record DTO 结构（含 Breaking Change），需要在所有下游调用方（AuthServiceImpl、AuthController、MenuServiceImpl、MenuController 等）修改前集中完成。GlobalErrorCode 扩展（14 个新增枚举值）是后续 Filter/Service 异常通信的编译期依赖——所有 Filter、LoginAttemptTracker、PasswordPolicy 等均依赖统一的 ErrorCode 枚举，先完成 ErrorCode 枚举扩展才能避免后续实现中临时新增枚举值的返工。

## 任务上下文

### LoginResponse 设计契约（OOD 5.2）
```java
public record LoginResponse(
    Long userId,
    String username,
    String accessToken,
    String refreshToken,
    String tokenType,           // "Bearer"
    long expiresIn,             // access token 固定有效期（秒）
    boolean passwordChangeRequired,
    UserInfoResponse user
) {}
```
- `expiresIn` 语义：access token 固定有效期（秒，从签发时刻起固定不变），供前端预估刷新时机，典型值 900

### UserInfoResponse 设计契约（OOD 5.2）
```java
public record UserInfoResponse(
    Long id,
    String username,
    String realName,            // 对应 User.nickname，UserConverter 中映射
    String phone,
    String email,
    String role,                // 主角色 code（按 Role.sort 升序取最高者）
    String position,            // 当前岗位 code
    Set<String> permissions     // 功能权限码集合
) {}
```

### TokenRefreshResponse 设计契约（OOD 5.2）
```java
public record TokenRefreshResponse(
    String accessToken,
    String refreshToken,
    String tokenType,           // "Bearer"
    long expiresIn
) {}
```
- 不含 user 字段。前端刷新 token 后应调用 `GET /api/auth/me` 获取最新用户信息。

### RefreshTokenRequest 设计契约（OOD 5.2）
```java
public record RefreshTokenRequest(
    @NotBlank String refreshToken
) {}
```
- 同时用于 refresh 端点（必选）和登出端点（`@RequestBody(required=false)` 可选携带）。

### PasswordChangeRequest 设计契约（OOD 5.2）
```java
public record PasswordChangeRequest(
    @NotBlank @Size(max = 128) String oldPassword,
    @NotBlank @Size(min = 8, max = 64) String newPassword
) {}
```

### ProfileUpdateRequest 设计契约（OOD 5.2）
```java
public record ProfileUpdateRequest(
    @NotBlank(message = "昵称不能为空") @Size(max = 50) String nickname,
    @Pattern(regexp = "^1[3-9]\\d{9}$") String phone,
    @Email @Size(max = 100) String email
) {}
```

### MenuCreateRequest 设计契约（OOD 5.2）
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

### MenuUpdateRequest 设计契约（OOD 5.2）
传统 POJO 类（非 record），PATCH 语义：
```java
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
    // getter/setter
}
```
`@JsonInclude(JsonInclude.Include.NON_NULL)`，PATCH 局部更新语义：请求体省略的字段保持不变。

### MenuResponse 设计契约
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

### GlobalErrorCode 扩展（OOD 10.2 节）
在已有 `SUCCESS, SYSTEM_ERROR, PARAM_INVALID, NOT_FOUND, UNAUTHORIZED, FORBIDDEN` 基础上新增：

| 枚举名 | code | message |
|--------|------|---------|
| LOGIN_FAILED | LOGIN_FAILED | 用户名或密码错误 |
| ACCOUNT_DISABLED | ACCOUNT_DISABLED | 账户已被管理员停用 |
| ACCOUNT_LOCKED | ACCOUNT_LOCKED | 账户已锁定，请{锁定时间}后重试 |
| RATE_LIMITED | RATE_LIMITED | 登录尝试过于频繁，请稍后重试 |
| RATE_LIMITED_GLOBAL | RATE_LIMITED_GLOBAL | 请求过于频繁，请稍后重试 |
| PASSWORD_TOO_SHORT | PASSWORD_TOO_SHORT | 密码长度不能少于8位 |
| PASSWORD_TOO_LONG | PASSWORD_TOO_LONG | 密码长度不能超过64位 |
| PASSWORD_WEAK | PASSWORD_WEAK | 密码不符合复杂度要求 |
| PASSWORD_CONTAINS_USERNAME | PASSWORD_CONTAINS_USERNAME | 密码不能包含用户名 |
| PASSWORD_COMMON | PASSWORD_COMMON | 密码过于常见 |
| TOKEN_REFRESH_FAILED | TOKEN_REFRESH_FAILED | 令牌刷新失败，请重新登录 |
| PASSWORD_CHANGE_REQUIRED | PASSWORD_CHANGE_REQUIRED | 需要修改密码 |
| CHILDREN_EXIST | CHILDREN_EXIST | 存在子菜单，无法删除 |
| PASSWORD_MISMATCH | PASSWORD_MISMATCH | 旧密码不正确 |

## 已有代码上下文

### 当前 DTO 状态
- 所有 DTO（`LoginRequest`、`LoginResponse`、`UserInfoResponse`、`MenuResponse`、`MenuCreateRequest`、`MenuUpdateRequest`、`ProfileUpdateRequest`）均为传统 POJO 类，使用手写 getter/setter（遵循 Phase 0 约定）。
- `LoginResponse` 含 `String token, String tokenType, Long expiresIn, UserInfoResponse user` 四个字段。
- `UserInfoResponse` 含 `..., String nickname, String userType, ...`，缺 `position`/`permissions`。
- 无 `RefreshTokenRequest`、`TokenRefreshResponse`、`PasswordChangeRequest` 等新 DTO。

### Breaking Change 调用方定位
**LoginResponse 调用方**（需将 `getToken()` → `accessToken()`，`getExpiresIn()` → `expiresIn()`）：
- `AuthServiceImpl.java`：构造并返回 `LoginResponse`
- `AuthController.java`：接收并返回 `LoginResponse`
- `AuthServiceTest.java` / `AuthControllerTest.java`：测试断言中访问 token

**UserInfoResponse 调用方**（需将 `getNickname()` → `realName()`，`getUserType()` → `role()`）：
- `AuthServiceImpl.java`：构建 `UserInfoResponse`
- `UserConverter.java`（待新建，或现有代码中构建 UserInfoResponse 的地方）
- `AuthController.java` / `MenuServiceImpl.java` 等引用 UserInfoResponse 的文件

**MenuResponse 调用方**：
- `MenuController.java`、`MenuServiceImpl.java`、`MenuConverter.java`

**其他记录类型的调用方**：
- `AuthController`：`LoginRequest`、`PasswordChangeRequest`、`RefreshTokenRequest` 作为 `@RequestBody` 参数；`LoginResponse`、`TokenRefreshResponse` 作为返回类型
- `AuthServiceImpl`：构造 DTO 的地方

### 当前 GlobalErrorCode
```java
public enum GlobalErrorCode implements ErrorCode {
    SUCCESS("SUCCESS", "成功"),
    SYSTEM_ERROR("SYSTEM_ERROR", "系统异常"),
    PARAM_INVALID("PARAM_INVALID", "参数校验失败"),
    NOT_FOUND("NOT_FOUND", "资源不存在"),
    UNAUTHORIZED("UNAUTHORIZED", "未认证"),
    FORBIDDEN("FORBIDDEN", "无权限");
    // ... 含 code/message 字段、构造器和 ErrorCode 接口实现
}
```
- 实现 `com.aimedical.common.exception.ErrorCode` 接口（需确认该接口是否声明 `getCode()`/`getMessage()` 方法）
- 新增枚举值需遵循同风格的构造器模式

### 不在此范围
- `Function`→`PermissionFunction` 重命名（独立任务，涉及 5 个文件级联修改）
- `JwtUtil`/`JwtTokenProvider` 改造（Phase 3 任务）
- `schema.sql` DDL 变更（独立任务）
- 集成测试用例新增（阶段 4 统一处理）
