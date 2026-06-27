# 任务指令（v13）

## 动作
NEW

## 任务描述
在 `common-module-api` 中创建 `UserFacade` 接口（含 `UserInfoResponse` 共享 DTO），在 `common-module-impl/auth/` 中创建 `UserFacadeImpl` 实现，为业务模块提供类型化的用户数据门面访问，消除对 `common-module-impl/permission/User` 实体的直接编译期依赖。

**新建文件**:
1. `modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/auth/UserInfoResponse.java` — record DTO，从 impl 迁至 api
2. `modules/common-module/common-module-api/src/main/java/com/aimedical/modules/commonmodule/auth/UserFacade.java` — 接口
3. `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/UserFacadeImpl.java` — @Component 实现
4. `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/UserFacadeImplTest.java` — 测试

**已有文件变更**:
5. `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/dto/response/UserInfoResponse.java` — 删除（已迁移至 api 模块），更新所有 import 引用

## 选择理由
- UserFacade 是 Stage 3 第二个 API 抽象，与 CurrentUser 职责互补
- 位于 common-module-api，业务模块可引用而不产生对 impl 模块的编译期依赖
- 是 AuthServiceImpl 中 UserInfoResponse 构建逻辑提取（UserConverter）的前置条件
- 依赖链：UserFacade → UserConverter → AuthServiceImpl，先创建 API 层抽象再向下推进

## 任务上下文

### UserFacade 接口
```
包路径：com.aimedical.modules.commonmodule.auth

public interface UserFacade {
    UserInfoResponse findById(Long userId);
    UserInfoResponse findByUsername(String username);
    boolean existsById(Long userId);
}
```

### UserInfoResponse（迁至 api 模块）
```
包路径：com.aimedical.modules.commonmodule.auth

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

### UserFacadeImpl
- `@Component`，包路径：`com.aimedical.modules.commonmodule.auth`
- 构造注入 `UserRepository`
- `findById(Long)`：委托 `userRepository.findById(id)` → 存在则构造 UserInfoResponse，不存在返回 null
- `findByUsername(String)`：委托 `userRepository.findByUsername(username)`（返回 Optional<User>）→ 存在则构造 UserInfoResponse，不存在返回 null
- `existsById(Long)`：委托 `userRepository.existsById(id)`

**UserInfoResponse 构建逻辑**：
- `id` → `user.getId()`
- `username` → `user.getUsername()`
- `realName` → `user.getNickname()`（字段映射：数据库 nickname → DTO realName）
- `phone` → `user.getPhone()`
- `email` → `user.getEmail()`
- `role` → 取用户主角色的 code（按 Role.sort 升序取第一个，无角色时返回空字符串）
- `position` → `user.getPosts().stream().findFirst().map(Post::getCode).orElse("")`
- `permissions` → 从 user.getRoles() + user.getPosts() → functions 收集权限码

### UserInfoResponse 迁移策略
- 原文件 `dto/response/UserInfoResponse.java` 删除
- `AuthServiceImpl.java`中引用 `dto.response.UserInfoResponse` 改为 `auth.UserInfoResponse`
- `MenuServiceImpl.java` 同样更新 import
- 其他引用处一并更新

## 已有代码上下文

### 依赖的已有类型
| 类型 | 所在模块 | 说明 |
|------|---------|------|
| `User` | `common-module-impl/permission/` | 用户实体，UserFacadeImpl 通过 UserRepository 查询 |
| `UserRepository` | `common-module-impl/permission/` | Spring Data JPA Repository |
| `Role` | `common-module-impl/permission/` | 角色实体，用于构建 role 字符串和权限 |
| `Post` | `common-module-impl/permission/` | 岗位实体，用于构建 position 字符串和权限 |
| `PermissionFunction` | `common-module-impl/permission/` | 功能实体，用于收集权限码 |
| `PositionEnum` | `common-module-api/api/` | 岗位枚举 |
| `UserType` | `common-module-api/api/` | 用户类型枚举 |

### UserFacadeImpl 测试设计
至少 6 个用例：
1. `findById_whenUserExists_shouldReturnUserInfo` — userRepository.findById(1L) 返回 Optional.of(user) → 返回完整 UserInfoResponse
2. `findById_whenUserNotFound_shouldReturnNull` — userRepository.findById(999L) 返回 Optional.empty() → 返回 null
3. `findByUsername_whenUserExists_shouldReturnUserInfo` — userRepository.findByUsername("doctor001") 返回 Optional.of(user) → 返回完整 UserInfoResponse
4. `findByUsername_whenUserNotFound_shouldReturnNull` — userRepository.findByUsername("nobody") 返回 Optional.empty() → 返回 null
5. `existsById_whenUserExists_shouldReturnTrue` — userRepository.existsById(1L) 返回 true → 返回 true
6. `existsById_whenUserNotFound_shouldReturnFalse` — userRepository.existsById(999L) 返回 false → 返回 false

关键细节：
- 使用 Mockito mock UserRepository
- `User` 实体需 stub getRoles() 返回 `Set<Role>`，getPosts() 返回 `Set<Post>` 以构建 role 和 position
- 权限收集的 Mock 可简化：stub roles/posts → stream → collect permission codes
- 测试包路径：`com.aimedical.modules.commonmodule.auth.UserFacadeImplTest`
