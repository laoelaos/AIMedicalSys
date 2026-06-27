# 任务指令（v16）

## 动作
NEW

## 任务描述
修复 T9：UserConverter 过滤逻辑修复 + UserFacadeImpl 委托

按 OOD 1.3 + 5.2 节设计契约：
1. **修复 UserConverter** 三处关键过滤逻辑缺失：
   - `resolveRole()` 补充 `.filter(Role::getEnabled)` 过滤已禁用角色
   - `resolveRole()` 改用 `Comparator.nullsLast(Comparator.naturalOrder())` 防止 sort 为 null 时 NPE
   - `resolvePermissions()` 两层循环补充 `PermissionFunction::getEnabled` 过滤
2. **UserFacadeImpl 委托 UserConverter**：注入 UserConverter，`toUserInfoResponse()` 委托 `userConverter.toUserInfoResponse(user)`，删除三个私有方法（`resolvePrimaryRole`、`resolvePosition`、`resolvePermissions`）
3. **测试适配**：UserConverterTest 新增禁用角色/null sort/禁用权限用例；UserFacadeImplTest 注入 UserConverter mock，验证委托行为正确

## 选择理由
P2 批次 6 第四个任务。T11（R15）已完成，T22/T23 已完成。T9 与 T8 存在隐含交叉（日志记录），但 T8 尚未启动；本任务按 OOD 1.3 + 5.2 节先修复 UserConverter 三处过滤逻辑后将转换职责收拢至 UserConverter，UserFacadeImpl 纯委托无日志需求，T8 后续引入审计日志时只需修改 UserConverter 单点。

## 任务上下文
### 三处修复（UserConverter.java）
**1. resolveRole()——Role::getEnabled 过滤 + null-safe sort：**
```java
// 当前（L41）
.min(Comparator.comparingInt(Role::getSort))
// 改为
.filter(Role::getEnabled)
.min(Comparator.comparing(Role::getSort, Comparator.nullsLast(Comparator.naturalOrder())))
```

**2. resolvePermissions()——PermissionFunction::getEnabled 过滤：**
当前 L63-68 的 `for (PermissionFunction function : post.getFunctions())` 循环体中添加过滤：
```java
// L65 当前
if (function != null && function.getCode() != null) {
// 改为
if (function != null && Boolean.TRUE.equals(function.getEnabled()) && function.getCode() != null) {
```
两处循环（L61-69 posts 循环、L75-85 roles→posts 循环）均需相同修改。

### UserFacadeImpl 委托改造
- 新增字段：`private final UserConverter userConverter;`
- 构造器新增参数：`UserConverter userConverter`
- `toUserInfoResponse()` 改为：
```java
private UserInfoResponse toUserInfoResponse(User user) {
    return userConverter.toUserInfoResponse(user);
}
```
- 删除 `resolvePrimaryRole`、`resolvePosition`、`resolvePermissions` 三个私有方法

### 测试适配
**UserConverterTest 新增测试：**
- `shouldFilterDisabledRole`：创建 enabled=false 的角色，验证返回空字符串
- `shouldHandleNullSort`：创建 sort=null 的角色，验证不抛 NPE
- `shouldFilterDisabledPermission`：创建 enabled=false 的 PermissionFunction，验证 permissions 中不包含

**UserFacadeImplTest 改造：**
- 构造器改传 `new UserFacadeImpl(userRepository, userConverter)`
- 新增 `UserConverter userConverter = mock(UserConverter.class);`
- 原有 findById/findByUsername 测试中 mock `when(userConverter.toUserInfoResponse(...))` 返回预期结果
- 原有断言不变

## 已有代码上下文
### UserConverter.java（当前现状）
- `resolveRole()`: 无 `Role::getEnabled` 过滤，`Comparator.comparingInt` 在 sort=null 时 NPE
- `resolvePermissions()`: 两层循环均无 `PermissionFunction::getEnabled` 过滤
- `UserFacadeImpl` 的对应方法已有正确实现，以此作为修复基准

### UserFacadeImpl 当前方法签名
```java
public UserFacadeImpl(UserRepository userRepository) { ... }
```

### 已有测试
- `UserConverterTest`（127 行，4 个测试）：toUserInfoResponse_shouldMapBasicFields、shouldMapRoleBySortPriority、shouldMapRoleToEmptyWhenNoRoles、shouldMapPositionFromFirstPost、shouldCollectPermissions
- `UserFacadeImplTest`（261 行，10 个测试）：覆盖 findById/findByUsername/existsById 正常/空/null/全禁用/权限合并场景

### 依赖约束
- `UserConverter` 无其他 Bean 依赖（纯 `@Component`），可安全注入
- `UserFacadeImpl` 当前仅依赖 `UserRepository`，新增 `UserConverter` 依赖不形成循环

## 文件路径
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/converter/UserConverter.java`（修改）
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/UserFacadeImpl.java`（修改）
- `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/converter/UserConverterTest.java`（修改）
- `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/UserFacadeImplTest.java`（修改）

## 验证方式
- `mvn clean test -pl common-module-impl -am` 或全项目 `mvn clean test`
- 验证全部 597+ 后端测试通过，重点关注 UserConverterTest 和 UserFacadeImplTest
- 前端 Vitest 失败为预存问题，不影响本任务验收
