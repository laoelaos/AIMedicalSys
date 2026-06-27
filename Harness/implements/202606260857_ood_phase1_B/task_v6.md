# 任务指令（v6）

## 动作
NEW

## 任务描述
新建 TokenBlacklist 接口与 InMemoryTokenBlacklist 内存实现，位于 `auth/blacklist/` 子包下。对应 OOD 第 12 节 Stage 2 任务 2.9（C4 登出 No-Op 修复）及 OOD 4.2 Phase 1 黑名单策略。

生产文件（2 个）：
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/blacklist/TokenBlacklist.java`
- `AIMedical/backend/modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/blacklist/InMemoryTokenBlacklist.java`

测试文件（1 个）：
- `AIMedical/backend/modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/blacklist/InMemoryTokenBlacklistTest.java`

## 选择理由
- TokenBlacklist 是 Stage 2 剩余基础设施中与已完成任务（RateLimitGuard 接口/实现、LoginAttemptTracker）形态最一致的任务——interface + 内存实现、无 Spring 依赖、`ConcurrentHashMap` 状态管理，可独立实现和全量单元测试覆盖
- 是 JwtAuthenticationFilter（2.1）和 AuthServiceImpl.logout()（3.6）的直接依赖，完成此任务后这些集成任务可独立推进
- OOD 4.2 已明确 Phase 1 的内存黑名单方案（仅 Access Token）、数据结构（`ConcurrentHashMap<String, Long>`）、清理策略（`ScheduledExecutorService` 每 5 分钟），实现边界清晰
- 不引入新外部依赖（仅 JDK），不与任何未完成任务产生耦合

## 任务上下文

### TokenBlacklist 接口（OOD 4.2 + 8.1 C4）
```java
package com.aimedical.modules.commonmodule.auth.blacklist;

public interface TokenBlacklist {
    void add(String jti, long expirationTime);
    boolean isBlacklisted(String jti);
}
```

- `add(String jti, long expirationTime)` — 将 token jti 加入黑名单；expirationTime 为 token 原始过期时间戳（epoch ms），供清理线程判断何时可安全移除
- `isBlacklisted(String jti)` — 检查 jti 是否在黑名单中；返回 true 表示 token 已被登出标记
- 接口不抛出业务异常，仅返回 boolean；业务异常由调用方（AuthServiceImpl）根据查询结果决定

### InMemoryTokenBlacklist 实现（Phase 1 方案）
- **数据结构**：`private final ConcurrentHashMap<String, Long> blacklist`，key=jti（UUID 字符串），value=expirationTime（epoch ms）
- **构造器**：`public InMemoryTokenBlacklist()` — 默认无参，初始化 ConcurrentHashMap 并启动 `ScheduledExecutorService` 清理线程
  - 清理线程：`Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "token-blacklist-cleanup"); t.setDaemon(true); return t; })`
  - 调度：`.scheduleWithFixedDelay(this::cleanup, 5, 5, TimeUnit.MINUTES)`（与 SlidingWindowCounter 风格一致，使用 daemon 线程）
- **add 实现**：`blacklist.put(jti, expirationTime)` — ConcurrentHashMap.put 本身线程安全
- **isBlacklisted 实现**：`blacklist.containsKey(jti)` — 注意：过期条目由定时线程异步清理，因此可能短暂存在「已过期但尚未清理」的中间状态，此行为可接受（token 原始过期时间已过，即使误判也为无害的误拒绝）
- **cleanup 实现**：遍历 `blacklist.entrySet()`，移除 `entry.getValue() < System.currentTimeMillis()` 的条目
- **线程安全**：ConcurrentHashMap 的 put/containsKey 本身线程安全；清理任务在 `SingleThreadScheduledExecutor` 中串行执行，不与业务线程产生并发冲突
- **不引入 ScheduledExecutorService 包装抽象**：与 SlidingWindowCounter 风格一致，直接在构造器中创建 executor 并启动调度；测试通过 package-private 构造器注入已构造的 ConcurrentHashMap 以跳过清理线程（或通过反射验证清理行为）
- **null key 行为**：ConcurrentHashMap 明确不支持 null key，所有方法对 null key 抛出 NPE 自然传播，与 LoginAttemptTracker 的 null key 策略一致
- **Phase 1 限制**：仅黑名单 Access Token jti，不黑名单 Refresh Token（因 Phase 1 内存方案的黑名单无法承受 Refresh Token 峰值 60M+ 条目的内存占用）；值类型为 Long（expirationTime）而非 Boolean，供清理线程判断何时可安全移除

### 已有代码上下文
- **包路径**：`com.aimedical.modules.commonmodule.auth.blacklist`（新建子包）
- **形态参考**：与 `auth/rateLimit/RateLimitGuard.java`（interface）+ `InMemoryRateLimitGuard.java`（实现）完全一致的 interface/impl 分离模式
- **ConcurrentHashMap + ScheduledExecutorService 模式**：与 `auth/rateLimit/SlidingWindowCounter.java` 的 executor 创建风格一致（daemon 线程、scheduleWithFixedDelay）
- **测试风格**：JUnit 5，不引入 Spring 上下文，与 `InMemoryRateLimitGuardTest` / `LoginAttemptTrackerTest` 同风格

### 测试覆盖（预计 12-15 用例）

| # | 测试方法 | 覆盖维度 | 验证点 |
|---|---------|---------|--------|
| 1 | `shouldReturnTrueForBlacklistedJti` | 正常路径 | add 后 isBlacklisted 返回 true |
| 2 | `shouldReturnFalseForUnknownJti` | 未知 key | 未添加的 jti 返回 false |
| 3 | `shouldHandleMultipleJtiIndependently` | 状态隔离 | 多个 jti 互不干扰 |
| 4 | `shouldReturnFalseAfterRemoval` | 移除后 | 过期清理后 isBlacklisted 返回 false（通过 package-private 构造器注入自定义 map 模拟过期场景） |
| 5 | `shouldReturnFalseForExpiredEntry` | 过期条目 | 手动设置过期时间戳早于 now，触发 cleanup 后 isBlacklisted 返回 false |
| 6 | `shouldThrowNpeWhenAddWithNullJti` | 错误路径 | add(null, exp) 抛 NPE |
| 7 | `shouldThrowNpeWhenIsBlacklistedWithNull` | 错误路径 | isBlacklisted(null) 抛 NPE |
| 8 | `shouldHandleConcurrentAddSameJti` | 并发安全 | 多线程并发 add 同一 jti，最终 isBlacklisted 返回 true |
| 9 | `shouldHandleConcurrentAddDifferentJti` | 并发安全 | 多线程并发 add 不同 jti，每个 jti 的 isBlacklisted 分别返回 true |
| 10 | `shouldNotThrowWhenAddingExistingJti` | 幂等性 | 重复 add 同一 jti 不抛异常，isBlacklisted 仍返回 true |
| 11 | `shouldCleanupExpiredEntries` | 定时清理 | 通过反射注入过期 entry，触发 cleanup 后 blacklist 中移除过期条目 |
| 12 | `shouldRetainNonExpiredEntriesAfterCleanup` | 清理保留 | 在过期条目清理后，未过期条目仍然存在 |
