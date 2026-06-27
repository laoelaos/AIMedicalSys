# 任务指令（v8）

## 动作
NEW

## 任务描述
修复 T5+T13：AuthServiceImpl 刷新流程 IP 失败计数 + dummy BCrypt 比对语义

涉及文件（1 个源文件）：
`AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/auth/service/impl/AuthServiceImpl.java`

### T5：refreshToken() 中用户禁用/删除时递增 IP 失败计数
- `refreshToken()` 方法（约 L180-184）在发现用户 `!enabled` 或 `deleted` 时直接抛出 `BusinessException(TOKEN_REFRESH_FAILED)`，未调用 `loginAttemptTracker.recordIpFailure(clientIp)`
- 需在 `throw new BusinessException(GlobalErrorCode.TOKEN_REFRESH_FAILED)` 之前插入 `loginAttemptTracker.recordIpFailure(getClientIp())`

### T13：login() 中 dummy BCrypt 使用 matches() 替代 encode()
- `login()` 方法中用户不存在场景（约 L105）和用户禁用/删除场景（约 L113）使用 `passwordEncoder.encode("dummy")`
- 需替换为 `passwordEncoder.matches("dummy", DUMMY_HASH)`
- 在类中定义虚拟哈希常量：`private static final String DUMMY_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";`

## 选择理由
Batch 4 剩余项，均修改 AuthServiceImpl.java（与 T3 同文件），P2 优先级，无阻塞依赖。

## 任务上下文
### T5 需求
OOD 3.1.3 步骤 7 明确规定："若用户已被禁用或被删除，需递增 LoginAttemptTracker IP 维度的失败计数，然后返回 TOKEN_REFRESH_FAILED"。缺少此调用降低了刷新场景下 IP 维度攻击检测的完整性。

### T13 需求
OOD 3.1.1 步骤 5/6 要求"对虚拟哈希值执行 dummy BCrypt 比对"以消除响应时间差异。encode() 语义不符合"比对"的设计意图，matches() 才是正确的比对方法。

## 已有代码上下文
- `AuthServiceImpl.java` 位于 `common-module-impl` 模块
- `loginAttemptTracker` 字段已注入（`LoginAttemptTracker` 类型），`recordIpFailure(String)` 方法已存在
- `passwordEncoder` 字段已注入（`PasswordEncoder` 类型），`matches(CharSequence, String)` 方法已存在
- `getClientIp()` 方法已存在（从 RequestContextHolder 获取客户端 IP）
- T3 的修改已在 `GlobalExceptionHandler` 和 `AuthServiceImpl` 中完成（login() 使用 `new BusinessException(GlobalErrorCode.ACCOUNT_LOCKED, ...)` 传入 args）

## 验证方式
```bash
mvn test -pl common-module/common-module-impl -am
```
预期：T5 场景——mock 用户 enabled=false 或 deleted=true，调用 refreshToken，验证 loginAttemptTracker.recordIpFailure() 被调用且参数为合理 IP
预期：T13 场景——对不存在的用户名和禁用用户场景，验证 login() 调用 passwordEncoder.matches() 而非 encode()
