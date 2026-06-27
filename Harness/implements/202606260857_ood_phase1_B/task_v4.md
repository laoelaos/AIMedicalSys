# 任务指令（v4）

## 动作
NEW

## 任务描述
实现 Stage 2 速率限制基础设施：新建 3 个类型。

### 1. SlidingWindowCounter（新建）
- **路径**：`AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/rateLimit/SlidingWindowCounter.java`
- **包**：`com.aimedical.modules.commonmodule.auth.rateLimit`
- **形态**：public class（工具类，非 Spring Bean）
- **职责**：滑动窗口计数器工具类，供 InMemoryRateLimitGuard 和 GlobalRateLimitFilter（后续任务）跨包复用
- **数据结构**：`ConcurrentHashMap<String, Deque<Long>>`，键为限流 key（IP 地址），值为时间戳滑动窗口 Deque
- **精度**：毫秒级时间戳，窗口清理精度 ±50ms
- **线程安全**：每个 key 的 Deque 在 `compute` 闭包内访问；`ReentrantLock` 保护跨窗口操作
- **公开方法**：
  - `boolean tryAcquire(String key, int limit, long windowMs)` — 窗口内计数 < 阈值则递增并返回 true，否则 false；原子性由 `ConcurrentHashMap.compute` 保证
- **过期清理**：`ScheduledExecutorService` 每 60 秒执行一次过期条目回收（清理 `Deque` 中超出窗口的旧时间戳，若 Deque 为空则移除 key）

### 2. RateLimitGuard（新建）
- **路径**：`AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/rateLimit/RateLimitGuard.java`
- **包**：`com.aimedical.modules.commonmodule.auth.rateLimit`
- **形态**：public interface
- **职责**：速率限制策略契约，支持 IP 级别的限流判定
- **方法**：`boolean tryAcquire(String key, int limit, long windowMs)`

### 3. InMemoryRateLimitGuard（新建）
- **路径**：`AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/rateLimit/InMemoryRateLimitGuard.java`
- **包**：`com.aimedical.modules.commonmodule.auth.rateLimit`
- **形态**：public class（implements RateLimitGuard）
- **职责**：内存限流实现。构造注入 `SlidingWindowCounter`；默认阈值 5 次 / 10 秒 / IP
- **方法**：`boolean tryAcquire(String key)` — 使用默认限流参数的便捷方法
- **错误码**：超出限制时返回 `ErrorCode.RATE_LIMITED`

### 单元测试
- **路径**：`AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/rateLimit/InMemoryRateLimitGuardTest.java`
- 覆盖正常路径（5 次以内放行）
- 覆盖限流触发（第 6 次拒绝）
- 覆盖窗口过期后重置（等待 >10 秒后恢复放行）
- 并发安全验证（多线程并发请求同一 key）

## 选择理由
这是 Stage 2 安全基础设施的底层依赖：
- SlidingWindowCounter 是滑动窗口算法的核心实现，被 InMemoryRateLimitGuard（本任务）和 GlobalRateLimitFilter（后续任务 2.2）共同依赖
- RateLimitGuard 接口定义了限流契约，使 AuthServiceImpl（Stage 3）可编程式调用
- InMemoryRateLimitGuard 实现登录端点专用限流（5次/10秒/IP），是三层防暴力破解的第一层
- 三者无外部依赖（仅 JDK + ErrorCode 枚举），可独立实现和验证

## 任务上下文
- **OOD 4.1 防暴力破解方案**（三层防护第一层：IP 级速率限制）：
  - 同一 IP（`/api/auth/login` 专用）：5 次 / 10 秒滑动窗口 → 429 + ErrorCode.RATE_LIMITED
  - 全局 IP 限流在 GlobalRateLimitFilter（后续任务）中实现，与 InMemoryRateLimitGuard 计数器实例独立
- **SlidingWindowCounter 契约**（OOD 4.1）：`ConcurrentHashMap<String, Deque<Long>>`；毫秒级精度；`tryAcquire` 原子操作
- **ErrorCode 区分策略**：InMemoryRateLimitGuard 返回 `RATE_LIMITED`；后续 GlobalRateLimitFilter 返回 `RATE_LIMITED_GLOBAL`

## 已有代码上下文
- **PermissionFunction.java**、**Post.java**、**Role.java**、**User.java**：已完成的包 A 实体（路径 `permission/`）
- **JwtUtil.java**：已完成 SecretKey 缓存（路径 `jwt/`），后续 Filter 基于 JwtTokenProvider（Stage 3）或 JwtUtil
- **SlidingWindowCounter 尚不存在**，需新建。GlobalRateLimitFilter（后续任务）将独立拥有自己的 `ConcurrentHashMap` 实例，不共享
- **包名层次**：`com.aimedical.modules.commonmodule.auth.rateLimit` — 该包不存在，需新建目录
- **项目使用 Java 21 + Spring Boot 3.2.5 + Maven**，测试框架为 JUnit 5

## 修订说明（v4 r1）
| 审查意见 | 修改措施 |
|---------|---------|
| [严重] plan.md 中 RateLimitGuard 方法签名写为 `boolean tryAcquire(String key)` 与 task 第 26 行不一致 | task_v4.md 内容正确无需修改，已修正 plan.md 第 53 行 |
| [一般] plan.md 中 InMemoryRateLimitGuard 提及 ReentrantLock 与 task 第 16 行不符 | task_v4.md 内容正确无需修改，已修正 plan.md 第 54 行 |
