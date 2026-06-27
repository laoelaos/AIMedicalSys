# 任务指令（v4）

## 动作
NEW

## 任务描述
**T11**: 去除 `AuthService.getCurrentUser(String token)` 的 token 参数，改为由 Controller 从 SecurityContext 获取 userId 后传入；Service 层不再重新解析 JWT。

涉及文件（5个）：
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/AuthService.java` — 接口签名变更
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/service/impl/AuthServiceImpl.java` — 实现重构
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/controller/AuthController.java` — 调用方变更
- `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/AuthServiceTest.java` — 测试更新
- `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/controller/AuthControllerTest.java` — 测试更新

## 选择理由
R3（T9+T10+T18）已验证通过（391/0）。T11 是计划中 R4 的任务，属于独立编码缺陷修复（架构实现偏差：Service 层重新解析 JWT 而非利用已装配的 SecurityContext）。涉及接口签名变更，需同步更新实现、Controller 和测试。与 R3 无文件重叠，可独立实施。

## 任务上下文

### OOD 要求
OOD 3.1.5 节规定："`GET /api/auth/me` 从 SecurityContext 获取当前用户 ID"。OOD 1.3 节定义的 `CurrentUser` 接口目标是"消除 Controller 层对 SecurityContextHolder 的直接操作"，但 OOD 3.1.5 的入口约定已明确指出从 SecurityContext 获取 userId 的方式。

### 当前缺陷
`AuthServiceImpl.getCurrentUser(String token)`（第 307-318 行）接收 token 参数，内部调用 `jwtTokenProvider.validateToken(token, null)` 重新解析 JWT Claims，再从中提取 userId。这种实现绕过 JwtAuthenticationFilter 已经装配的 SecurityContext，存在 token 验证与 Filter 不一致风险（如 Filter 拒绝但 Service 放行，或 Filter 放行但 Service 拒绝）。

### Controller 已有模式
`AuthController.changePassword()`（第 81-87 行）已使用 `getCurrentUserId()` 从 SecurityContext 提取 userId，然后调用 `authService.changePassword(userId, ...)`。`AuthController.me()`（第 59-67 行）应遵循相同模式：调用 `getCurrentUserId()` 获取 userId 后传入 Service，而非从 header 提取 token 再传递。

### 已有实现参考

**AuthController.java:89-98** — `getCurrentUserId()` 方法已存在：
```java
private Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Object principal = authentication.getPrincipal();
    if (principal instanceof Long) {
        return (Long) principal;
    }
    if (principal instanceof Integer) {
        return ((Integer) principal).longValue();
    }
    throw new IllegalStateException("无法从SecurityContext获取用户ID");
}
```

### 修复规范
1. `AuthService.getCurrentUser(String token)` → `AuthService.getCurrentUser(Long userId)` — 接口签名变更
2. `AuthServiceImpl.getCurrentUser()` — 删除 token 解析代码，直接 `userRepository.findById(userId)`，原有 BusinessException 保留（用户不存在场景）
3. `AuthController.me()` — 使用 `getCurrentUserId()` 获取 userId，传递给 `authService.getCurrentUser(userId)`
4. `AuthServiceTest.getCurrentUser_shouldSucceed()` — 从 `authService.getCurrentUser("token")` 改为 `authService.getCurrentUser(1L)`
5. `AuthControllerTest` — `when(authService.getCurrentUser("mock-token"))` 改为 `when(authService.getCurrentUser(1L))`

### 不修改范围
- `MenuController.getCurrentUserId()`（第 152 行）——属于 T19，将在 R6 中修复
- `AuthService.updateProfile()` 的 token 参数——不在 T11 范围内
