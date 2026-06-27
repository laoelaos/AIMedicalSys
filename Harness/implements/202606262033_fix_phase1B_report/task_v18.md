# 任务指令（v18）

## 动作
NEW

## 任务描述
测试补充：`AuthServiceTest.java` 新增测试方法 `login_shouldThrowLoginFailed_whenUserDeleted()`，mock 一个 `deleted=true` 的 `User`，验证 `login()` 抛出 `BusinessException(GlobalErrorCode.LOGIN_FAILED)`。仅修改 `AuthServiceTest` 一个文件。

## 选择理由
批次 7 首个任务（测试补充序列）。T15 是 P3 测试覆盖补充，无任何代码前置依赖（`AuthServiceImpl.login()` 已正确处理 `deleted=true` 场景，见 L112 `Boolean.TRUE.equals(user.getDeleted())`，仅缺少独立测试覆盖）。T15 与 T16-T18 互不依赖，适合作为批次 7 的起始任务，验证批次 6 全部功能修复后的测试基线稳定性。

## 任务上下文
**OOD 3.1.1 节步骤 6**：用户 `enabled == false` 或 `deleted == true` → 对虚拟哈希值执行 dummy BCrypt 比对以消除与步骤 7 的响应时间差异 → 递增 LoginAttemptTracker 用户名和 IP 双维度的失败计数 → 抛出 `BusinessException(ErrorCode.LOGIN_FAILED，消息"用户名或密码错误")`。

**诊断报告 §T15**：
- `AuthServiceTest`（`service/AuthServiceTest.java:141-155`）的 `login_shouldThrowUserDisabled()` 仅测试了 `enabled=false` 场景
- 缺少 `deleted=true` 场景的独立测试覆盖
- 修改方式：参照 `login_shouldThrowUserDisabled()`，mock 一个 `deleted=true` 的 User，验证 login() 抛出 `BusinessException(LOGIN_FAILED)`

## 已有代码上下文

### AuthServiceTest.java 现有 deleted 测试
```java
// AuthServiceTest.java L141-155 (approximate location)
void login_shouldThrowUserDisabled() {
    User disabledUser = new User();
    disabledUser.setEnabled(false);
    disabledUser.setDeleted(false);
    // ...mock 设置和断言
}
```

### 已有 enabled=false 测试参考模式
- mock User 对象设置 `setEnabled(false)` `setDeleted(false)`
- mock UserRepository.findByUsername() 返回该 User
- mock BCryptPasswordEncoder.matches()（消除响应时间差异）
- mock LoginAttemptTracker ip 和 username 维度锁定检查返回 false
- 调用 `authService.login(request)`
- 验证 `BusinessException` 的 `errorCode` 为 `GlobalErrorCode.LOGIN_FAILED`

### 当前测试基线
mvn clean test 全部 608 测试通过，0 失败 5 跳过，BUILD SUCCESS。AuthServiceTest 已适配 SecurityAuditLogger 注入（第 11 个构造参数）。

### 相关文件路径
- `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/service/AuthServiceTest.java`
