# 详细设计（v11）

## 概述

修复 `Docs/05_ood_phase1_B.md` 中的 5 处 OOD 文档缺陷：T8（BusinessException args 示例值错误）、T2-OOD（4.2 节缺少异常刷新阻断逻辑定义）、T12-OOD（4.7 节密钥字符集迁移策略不明确）、T13-OOD（4.1 节锁粒度描述矛盾）、T17-OOD（10.3 节缺少 AuthenticationEntryPoint/AccessDeniedHandler 插值出口）。仅修改 `Docs/05_ood_phase1_B.md` 一个文件，无代码变更。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `Docs/05_ood_phase1_B.md` | MODIFY | 5 处局部文本修改 |

## 变更详述

### T8 — 修正 BusinessException args 示例值

**位置 1**：第 155 行（3.1.1 节末尾段落）

**原文**：
```
构造 `BusinessException(GlobalErrorCode.ACCOUNT_LOCKED, "请30分钟后重试")` 或 `"请15分钟后重试"`，第二个参数作为 args 传入。
```

**改为**：
```
构造 `BusinessException(GlobalErrorCode.ACCOUNT_LOCKED, "30分钟")` 或 `"15分钟"`，第二个参数作为 args 传入。
```

**位置 2**：第 1221-1224 行（10.3 节 args 示例代码块）

**原文**：
```java
// IP 维度锁定
throw new BusinessException(GlobalErrorCode.ACCOUNT_LOCKED, "请30分钟后重试");
// 用户名维度锁定
throw new BusinessException(GlobalErrorCode.ACCOUNT_LOCKED, "请15分钟后重试");
```

**改为**：
```java
// IP 维度锁定
throw new BusinessException(GlobalErrorCode.ACCOUNT_LOCKED, "30分钟");
// 用户名维度锁定
throw new BusinessException(GlobalErrorCode.ACCOUNT_LOCKED, "15分钟");
```

### T2-OOD — 补充异常刷新检测阻断逻辑（4.2 节）

**位置**：第 502 行（4.2 节 Refresh Token 安全补偿策略·异常刷新检测段落）

**原文**（概要）：
```
- **异常刷新检测**：在 `AuthServiceImpl.refresh()` 方法中增加刷新频率检测逻辑。定义时间窗口为 5 秒、阈值为 2 次：若同一 `userId` 在 5 秒内出现 2 次以上刷新请求（暗示旧 Refresh Token 被重复使用），触发安全告警。检测逻辑使用 `ConcurrentHashMap<Long, Deque<Long>>` 维护每个用户的刷新时间戳滑动窗口，窗口过期条目惰性清除。告警方式为 `log.warn(...)` 输出安全日志，对接业务监控系统（如 Prometheus + AlertManager）消费此日志。Phase 2 配合 Redis 黑名单后此检测可作为辅助保留或移除
```

**改为**（加粗部分为新增内容）：
```
- **异常刷新检测与阻断**：在 `AuthServiceImpl.refresh()` 方法中增加刷新频率检测逻辑。定义时间窗口为 5 秒、阈值为 2 次：若同一 `userId` 在 5 秒内出现 2 次以上刷新请求（暗示旧 Refresh Token 被重复使用），触发安全告警并**拒绝本次刷新请求，返回 TOKEN_REFRESH_FAILED 错误并强制前端清除本地 token。阻断逻辑插入位置：在步骤 3（验证 Refresh Token 有效性）之后、步骤 4（检查黑名单）之前——即发现异常刷新时立即阻断，不继续执行后续查库和签发逻辑。检测逻辑使用 `ConcurrentHashMap<Long, Deque<Long>> refreshTimestamps` 维护每个用户的刷新时间戳滑动窗口，窗口过期条目通过惰性清除（在每次插入新时间戳时移除窗口外的旧条目）和 `ScheduledExecutorService` 定期清理（每 60 秒扫描一次，移除所有窗口外的过期时间戳）两种方式回收内存**。告警方式为 `log.warn("Suspicious refresh pattern detected for userId: {}, {} refreshes in {} seconds", userId, count, window)` 输出安全日志，与阻断逻辑同时触发。对接业务监控系统（如 Prometheus + AlertManager）消费此日志。Phase 2 配合 Redis 黑名单后此检测可作为辅助保留或移除
```

### T12-OOD — 明确 URL-safe 密钥字符集迁移策略（4.7 节）

**位置 1**：第 635-638 行（4.7 节启动验证逻辑描述）

**原文**：
```
1. 检查 secret 是否为 null 或空字符串 → 抛出 IllegalStateException("JWT_SECRET must be configured")
2. 检查 Base64 解码后的字节长度 ≥ 32 → 不满足则抛出 IllegalStateException("JWT_SECRET must be at least 256 bits (32 bytes) after Base64 decoding")
3. 检查 secret 是否仅包含 Base64 URL-safe 字符 → 不满足则抛出 IllegalStateException("JWT_SECRET contains invalid characters")
```

**改为**（与实际代码顺序对齐，并调整错误消息）：
```
1. 检查 secret 是否为 null 或空字符串 → 抛出 IllegalStateException("JWT secret must be configured")
2. 检查 secret 是否仅包含 URL-safe Base64 字符集（A-Z a-z 0-9 - _）→ 不满足则抛出 IllegalStateException("JWT secret contains invalid URL-safe Base64 characters")
3. 使用 `Base64.getUrlDecoder().decode(secret)` 解码 → 解码失败（非标准 URL-safe Base64 格式）则抛出 IllegalStateException("JWT secret is not a valid Base64 string: ...")
4. 检查解码后的字节长度 ≥ 32 → 不满足则抛出 IllegalStateException("JWT secret must decode to at least 32 bytes (256 bits), got: ...")
```

**位置 2**：在第 623 行（合法字符集约束）之后或第 639 行之前，新增以下段落：

**新增内容**：
```
#### 从标准 Base64 迁移的过渡策略

生产环境若已有使用标准 Base64 格式（含 `+`、`/`、`=` padding）的 JWT_SECRET，需按以下方案之一迁移：

**方案 A：双密钥过渡（推荐）**
1. 生成新的 URL-safe Base64 密钥（`openssl rand -base64 48 | tr '+/' '-_' | tr -d '='`）
2. 在配置中增加 `jwt.secret.fallback` 属性，保留旧密钥作为回退密钥
3. `JwtTokenProvider.init()` 加载主密钥（URL-safe），同时预解析回退密钥并存为备用 `SecretKey`
4. `validateToken()` 验证时先用主密钥解析，失败则尝试回退密钥
5. 所有 token 在自然过期（最长 7 天）后，移除回退密钥配置
6. 部署时间窗口：7 天（Refresh Token 最大有效期）

**方案 B：一次性重新生成（简单，有服务中断）**
1. 生成新的 URL-safe 密钥
2. 直接替换配置中的 JWT_SECRET 并重启服务
3. 所有已签发的 token 即时失效（包括 Refresh Token）
4. 用户需重新登录
5. 部署时间窗口：分钟级（重启后立即可用）

**推荐方案 A**，避免强制全量重新登录。方案 A 的 fallback 机制在 Phase 1 代码中不实现，仅作为配置备选方案的建议；Phase 1 采用方案 B（OOD 文档中仅记录策略说明，代码层面仅支持 URL-safe 单密钥）。
```

### T13-OOD — 澄清锁粒度描述（4.1 节）

**位置 1**：第 433 行（段落描述）

**原文**：
```
滑动窗口以 `ReentrantLock` 保护窗口内的排序集合，确保每个 IP 的窗口对象独立加锁。限流检查仅发生在登录请求，并发量低，锁竞争可忽略。
```

**改为**：
```
每个 key 的 Deque 在 `ConcurrentHashMap.compute` 闭包内原子访问，无需额外锁。限流检查仅发生在登录请求，并发量低，锁竞争可忽略。
```

**位置 2**：第 444 行（SlidingWindowCounter 契约·线程安全）

**原文**：
```
- **线程安全**：每个 key 的 Deque 在 `compute` 闭包内访问，`ReentrantLock` 保护跨窗口操作的原子性（如"检查+新增"复合操作）
```

**改为**：
```
- **线程安全**：每个 key 的 Deque 在 `ConcurrentHashMap.compute` 闭包内原子访问（如"检查+新增"复合操作），无需额外锁
```

### T17-OOD — 补充 AuthenticationEntryPoint/AccessDeniedHandler 插值出口（10.3 节）

**位置**：在第 1213 行（管线图末尾）之后、第 1215 行（关键设计决策）之前，新增以下内容：

**新增内容**：
```
        ↓
RestAuthenticationEntryPoint 和 RestAccessDeniedHandler 分支
        ↓ (模板插值)
GlobalExceptionHandler.handleBusinessException() 统一出口（共享 MessageInterpolator）
        ↓
Result.fail(errorCode, message) → JSON 响应体
```

**并在第 1217 行之后（第 2 项决策说明之前）或第 1227 行之后，新增以下独立段落**：

```
7. **AuthenticationEntryPoint/AccessDeniedHandler 插值出口**：`RestAuthenticationEntryPoint`（处理 AuthenticationException）和 `RestAccessDeniedHandler`（处理 AccessDeniedException）在调用 `Result.fail(errorCode)` 之前，需通过 `MessageInterpolator` 完成消息模板插值。这两个出口与 `GlobalExceptionHandler` 共享同一个 `MessageInterpolator` 组件。

8. **MessageInterpolator 组件定义**：

   **形态**：interface + 实现类
   **包路径**：`com.aimedical.common.util`
   **职责**：将模板字符串与 args 数组插值为最终消息文本，供全局异常处理器和安全出口共享使用

   **接口定义**：
   ```java
   public interface MessageInterpolator {
       String interpolate(String template, Object[] args);
   }
   ```

   **实现类**：`SimpleMessageInterpolator`（已有）—— 先尝试 `MessageFormat.format`（检测到 `{数字}` 模式时），否则使用 `String.replace` 或 `replaceFirst` 处理命名占位符。

   **使用方式**：`GlobalExceptionHandler`、`RestAuthenticationEntryPoint`、`RestAccessDeniedHandler` 均注入同一个 `SimpleMessageInterpolator` Bean，在构造 `Result.fail()` 之前调用 `interpolate(errorCode.getMessage(), args)`。

9. **管线全貌**（更新版）：

   ```
   BusinessException 构造
       ↓ (args 传递动态参数)
   ┌──────────────────────────────────────────────────┐
   │ Service 层抛出                                   │
   │ (如 AuthServiceImpl.login() / refresh())          │
   └──────────────┬───────────────────────────────────┘
                  ↓
   ┌──────────────────────────────────────────────────┐
   │ GlobalExceptionHandler.handleBusinessException() │
   │ RestAuthenticationEntryPoint                     │
   │ RestAccessDeniedHandler                          │
   │         ↓ (共享 MessageInterpolator)               │
   │ SimpleMessageInterpolator.interpolate(template,  │
   │     args) → 插值后消息文本                        │
   └──────────────────────┬───────────────────────────┘
                          ↓
   Result.fail(errorCode, message) → JSON 响应体
   ```

   **设计说明**：
   - `RestAuthenticationEntryPoint` 触发场景：未认证请求访问受保护资源，token 无效/过期/缺失，或已认证用户被禁用（enabled=false，携带 ACCOUNT_DISABLED 错误码）。EntryPoint 从 AuthenticationException 中提取 ErrorCode，调用 `MessageInterpolator.interpolate()` 处理消息模板，然后构造 `Result.fail()` 响应。
   - `RestAccessDeniedHandler` 触发场景：已认证用户权限不足，或 PasswordChangeCheckFilter 抛出 PasswordChangeRequiredException。Handler 从 AccessDeniedException 中提取 ErrorCode，调用 `MessageInterpolator.interpolate()` 处理消息模板，然后构造 `Result.fail()` 响应。
   - 三个出口（GlobalExceptionHandler / RestAuthenticationEntryPoint / RestAccessDeniedHandler）共享同一个 `MessageInterpolator` Bean，确保消息插值逻辑一致，避免重复实现。
```

## 错误处理

| 场景 | 处理方式 |
|------|---------|
| OOD 文档修改后与代码实现不一致 | 逐行对照实际代码验证；本次 T12 启动验证顺序已与 JwtTokenProvider.init() 确认一致 |
| 新增的描述与现有段落语义冲突 | 需确保补充内容与原文不矛盾（如 T13 删除矛盾描述后再新增） |

## 行为契约

- 所有修改限于 `Docs/05_ood_phase1_B.md` 一个文件
- 修改后文档的描述必须与实际代码实现一致
- T2 描述的阻断逻辑（TOKEN_REFRESH_FAILED）在 Phase 1 代码中**尚未实现**，OOD 文档中新增的设计需标记为"待实施"或与代码状态对齐

## 依赖关系

| 依赖 | 说明 |
|------|------|
| `Docs/05_ood_phase1_B.md` | 唯一目标文件 |
| `JwtTokenProvider.java` | T12 验证代码参考（确认启动验证顺序） |
| `SlidingWindowCounter.java` | T13 确认实际使用 `ConcurrentHashMap.compute` 而非 ReentrantLock |
| `SimpleMessageInterpolator.java` | T17 引用已有实现 |
