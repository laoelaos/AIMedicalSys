# 详细设计（v6）

## 概述

实现 Stage 2 登出 No-Op 修复的核心基础设施：TokenBlacklist 接口与 InMemoryTokenBlacklist 内存实现。新建 1 个接口、1 个生产类、1 个测试类，位于 `auth/blacklist/` 子包下。属于 OOD 4.2（Token 黑名单策略）与 OOD 8.1 C4（登出 No-Op 修复）的 Phase 1 内存方案。

设计目标：
- 封装 Token jti 黑名单与过期条目标记的内存状态管理逻辑，与 RateLimitGuard / InMemoryRateLimitGuard 同属 Stage 2 底层基础设施
- 提供可注入的测试构造器（package-private），使单元测试能跳过后台清理线程直接验证清理行为
- 接口与 OOD 3.1.2 步骤 3（JwtAuthenticationFilter 查询黑名单）和 OOD 3.1.4 步骤 2（AuthServiceImpl 登出加黑名单）的契约严格一致

不在范围：Refresh Token 黑名单（Phase 2 Redis 方案）、AuthServiceImpl 集成调用（Stage 3）、JwtAuthenticationFilter 集成调用（Stage 2 任务 2.1）。

## 文件规划

| 文件路径（相对 `AIMedical/backend/`） | 操作 | 职责 |
|---------|------|------|
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/blacklist/TokenBlacklist.java` | 新建 | Token 黑名单查询接口 |
| `modules/common-module/common-module-impl/src/main/java/com/aimedical/modules/commonmodule/auth/blacklist/InMemoryTokenBlacklist.java` | 新建 | 内存黑名单实现（Phase 1，仅 Access Token） |
| `modules/common-module/common-module-impl/src/test/java/com/aimedical/modules/commonmodule/auth/blacklist/InMemoryTokenBlacklistTest.java` | 新建 | InMemoryTokenBlacklist 单元测试（12 用例） |

## 类型定义

### TokenBlacklist

**形态**：public interface
**包路径**：`com.aimedical.modules.commonmodule.auth.blacklist`
**职责**：Token 失效状态查询的抽象，定义黑名单 jti 的添加与查询契约。

```java
public interface TokenBlacklist {
    void add(String jti, long expirationTime);
    boolean isBlacklisted(String jti);
}
```

**公开接口**：

| 方法签名 | 返回 | 说明 |
|---------|------|------|
| `void add(String jti, long expirationTime)` | void | 将 token jti 加入黑名单；expirationTime 为 token 原始过期时间戳（epoch ms），供清理线程判断何时可安全移除 |
| `boolean isBlacklisted(String jti)` | boolean | 检查 jti 是否在黑名单中；返回 true 表示 token 已被登出标记 |

**构造方式**：不直接实例化，由实现类提供构造器。
**类型关系**：独立接口，无继承关系；供 AuthServiceImpl（登出）、JwtAuthenticationFilter（验证）等下游类型使用。

### InMemoryTokenBlacklist

**形态**：public class（非 Spring Bean，后续 AuthModuleConfig 可显式注册为 @Bean）
**包路径**：`com.aimedical.modules.commonmodule.auth.blacklist`
**职责**：TokenBlacklist 的内存实现，使用 `ConcurrentHashMap<String, Long>` 存储黑名单 jti，通过 `ScheduledExecutorService` 每 5 分钟清理过期条目。

```java
public class InMemoryTokenBlacklist implements TokenBlacklist {

    private final ConcurrentHashMap<String, Long> blacklist;

    public InMemoryTokenBlacklist()
    InMemoryTokenBlacklist(ConcurrentHashMap<String, Long> blacklist)
}
```

**字段说明**：
- `blacklist` — `private final ConcurrentHashMap<String, Long>`，key=jti（UUID 字符串），value=expirationTime（epoch ms）；与 SlidingWindowCounter / InMemoryRateLimitGuard 的字段可见性约定一致
- `cleanupExecutor` — 不作为实例字段存储（见下方构造器行为），在公开构造器中以局部变量形式创建并调度，避免测试构造器被迫初始化 final 字段

**构造器**：

| 构造器签名 | 可见性 | 说明 |
|-----------|--------|------|
| `public InMemoryTokenBlacklist()` | public | 生产 API，初始化空 ConcurrentHashMap 并启动 ScheduledExecutorService 清理线程 |
| `InMemoryTokenBlacklist(ConcurrentHashMap<String, Long> blacklist)` | package-private | 测试构造器，接受已构造的 map 实例作为数据源，**不启动** ScheduledExecutorService；同包测试类 `InMemoryTokenBlacklistTest` 可见，可注入预填数据的 map 以验证清理/查询行为 |

`public InMemoryTokenBlacklist()` 构造器行为：
1. 初始化 `this.blacklist = new ConcurrentHashMap<>()`
2. 创建 `ScheduledExecutorService` **局部变量**（与 SlidingWindowCounter 风格一致，使用 daemon 线程）——不作为实例字段存储，局部变量超出构造器作用域后 GC 不可达，但已创建的 daemon 线程持有 executor 的内部引用，保证 executor 不被 GC 且清理任务持续运行：

```java
ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r, "token-blacklist-cleanup");
    t.setDaemon(true);
    return t;
});
```

3. 调度清理任务：`cleanupExecutor.scheduleWithFixedDelay(this::cleanup, 5, 5, TimeUnit.MINUTES)`

**公开接口**（实现 TokenBlacklist 接口 + 附加方法）：

| 方法签名 | 返回 | 说明 |
|---------|------|------|
| `void add(String jti, long expirationTime)` | void | 生产构造器路径：`blacklist.put(jti, expirationTime)`，ConcurrentHashMap.put 本身线程安全 |
| `boolean isBlacklisted(String jti)` | boolean | `blacklist.containsKey(jti)`，当前容器中已添加（含已过期但尚未被定时清理线程移除）的 jti 返回 true |
| `void cleanup()` | void | **package-private**（测试用例 4/5/11/12 需要直接调用 `cleanup()` 验证清理行为，测试与生产同包故无需 public 即可访问；`this::cleanup` 在同一类中使用包私有方法引用完全合法）；遍历 `blacklist.entrySet()`，移除 `entry.getValue() < System.currentTimeMillis()` 的条目 |

**方法行为约束**：

`add(String jti, long expirationTime)`：
- `ConcurrentHashMap.put(key, value)` 覆盖语义：重复 add 同一 jti 时，新 expirationTime 覆盖旧值；幂等，不抛异常
- 无返回值，不抛出业务异常

`isBlacklisted(String jti)`：
- `ConcurrentHashMap.containsKey(key)` 语义：已添加的 key（含已过期但尚未被清理的）返回 true，未添加的 key 返回 false
- 已过期但尚未被清理的条目返回 true，此行为为已知设计（token 原始过期时间已过，即使误判也为无害的误拒绝）

`cleanup()`：
- 遍历 `blacklist.entrySet()`，移除 `entry.getValue() < System.currentTimeMillis()` 的条目
- ConcurrentHashMap 的 `entrySet().removeIf()` 在 JDK 8+ 中非原子操作，但 cleanup 在 `SingleThreadScheduledExecutor` 中串行执行，不与业务线程（put/containsKey）产生 ConcurrentModificationException；`removeIf` 底层使用迭代器的 `remove` 方法

**类型关系**：实现 `TokenBlacklist` 接口，与 SlidingWindowCounter / InMemoryRateLimitGuard 无共享状态或数据结构。

**ScheduledExecutorService 关闭**：当前 Phase 1 生命周期管理由 JVM 关闭守护线程自动终止，不提供显式 `shutdown()` 方法（`cleanupExecutor` 为构造器局部变量，由 daemon 线程内部引用保证存活，与 SlidingWindowCounter 字段持有方式不同但生命周期逻辑一致）。

## 错误处理

`java.util.concurrent.ConcurrentHashMap` 明确不支持 null key（`put` / `containsKey` 等方法 Javadoc 均声明 `Throws: NullPointerException - if the specified key is null`）。InMemoryTokenBlacklist 选择**保持 JDK 一致行为**：所有方法对 null key 抛出 NPE 自然传播，不引入显式 null 守卫（与 LoginAttemptTracker 的 null key 策略一致）。调用方（JwtAuthenticationFilter / AuthServiceImpl）负责传入非 null 的 jti 字符串。

| 方法 | 错误场景 | 处理方式 |
|------|---------|---------|
| `add` | `jti` 为 null | `ConcurrentHashMap.put` 抛出 `NullPointerException`，自然传播 |
| `isBlacklisted` | `jti` 为 null | `ConcurrentHashMap.containsKey` 抛出 `NullPointerException`，自然传播 |
| `add` | `jti` 已存在 | ConcurrentHashMap.put 幂等覆盖，无异常 |
| `isBlacklisted` | jti 存在但已过期未清理 | 返回 true（已知设计：无害的误拒绝） |

InMemoryTokenBlacklist 本身不引用 `BusinessException` / `GlobalErrorCode`，黑名单状态仅以 boolean 形式返回。

## 行为契约

### InMemoryTokenBlacklist 整体契约
- **线程安全**：`ConcurrentHashMap.put` 和 `containsKey` 本身线程安全；清理任务在 `SingleThreadScheduledExecutor` 中串行执行，不与业务线程产生并发冲突
- **过期清理**：cleanup 每 5 分钟执行一次，移除 `expirationTime < now` 的条目；过期条目可能在过期后最多 5 分钟内仍被误判（已知设计，无害的误拒绝）
- **Phase 1 限制**：仅黑名单 Access Token jti，不黑名单 Refresh Token；值类型为 Long（expirationTime）而非 Boolean，供 cleanup 判断何时可安全移除
- **null key 一致性**：所有公开方法对 null key 一致抛 NPE 自然传播（与 LoginAttemptTracker 的 null key 策略一致）
- **不抛业务异常**：仅返回 boolean 状态；业务异常由调用方决定

### add(String jti, long expirationTime)
- **前置条件**：`jti` 非 null（否则 NPE 自然传播），`expirationTime` 为正整数 epoch ms（不对 expirationTime 做合法性校验）
- **后置条件**：jti 加入黑名单 Map；若已存在则覆盖 expirationTime
- **幂等性**：重复 add 同一 jti 不抛异常，最终 isBlacklisted 返回 true

### isBlacklisted(String jti)
- **前置条件**：`jti` 非 null（否则 NPE 自然传播）
- **后置条件**：
  - 未知 jti → 返回 false
  - jti 在黑名单中（含已过期但未清理） → 返回 true
- **副作用**：无（纯查询，不修改容器）

### cleanup()
- **前置条件**：无（可安全重复调用）
- **后置条件**：所有 `expirationTime < System.currentTimeMillis()` 的条目从 blacklist 中移除
- **执行环境**：由 ScheduledExecutorService 每 5 分钟调用一次；测试中可主动调用

### 并发场景
- 多线程并发 `add` 同一 jti：ConcurrentHashMap.put 最后写入者胜出，最终 isBlacklisted 返回 true
- 多线程并发 `add` 不同 jti：各自写入，互不干扰
- cleanup 与 add 并发：cleanup 移除过期条目的同时添加操作仍在运行（removeIf 与 put 在 ConcurrentHashMap 中安全共存，不会产生 ConcurrentModificationException）

## 依赖关系

### 新增依赖（生产代码）
- `java.util.concurrent.ConcurrentHashMap` — 数据结构
- `java.util.concurrent.Executors` — 清理线程池创建
- `java.util.concurrent.ScheduledExecutorService` — 定时清理
- `java.util.concurrent.TimeUnit` — 调度时间单位
- `java.lang.System` / `System.currentTimeMillis()` — 时间戳来源

### 内部新增类型
- `InMemoryTokenBlacklist`（class）— TokenBlacklist 内存实现

### 已有依赖（不变）
- 无外部依赖；不引用 `BusinessException` / `GlobalErrorCode`（由调用方 AuthServiceImpl / JwtAuthenticationFilter 处理）
- 与 `SlidingWindowCounter` / `InMemoryRateLimitGuard` / `LoginAttemptTracker` 无共享状态或数据结构

### 测试代码新增依赖
- `org.junit.jupiter.api.Test`
- `org.junit.jupiter.api.BeforeEach`（可选，构造预填 map）
- `java.lang.reflect.Field` — 反射访问 `private final` map 字段
- `java.util.concurrent.CountDownLatch` / `CyclicBarrier`（并发测试）
- `java.util.concurrent.atomic.AtomicBoolean`（并发测试验证）

### 暴露给后续任务的公开接口
- `InMemoryTokenBlacklist()` 无参构造器 — 供 AuthModuleConfig 显式注册为 @Bean
- `add(String, long)` / `isBlacklisted(String)` — 供 AuthServiceImpl.logout()（OOD 3.1.4 步骤 2）和 JwtAuthenticationFilter（OOD 3.1.2 步骤 3）调用
- **package-private 测试构造器** `InMemoryTokenBlacklist(ConcurrentHashMap<String, Long>)` — 仅供同包 `InMemoryTokenBlacklistTest` 使用

### 不在此范围
- AuthServiceImpl 登出集成调用（Stage 3 任务 3.6）
- JwtAuthenticationFilter 黑名单查询集成（Stage 2 任务 2.1）
- RedisTokenBlacklist 实现（Phase 2）
- ScheduledExecutorService 生命周期管理（JVM 关闭守护线程自动终止，与 SlidingWindowCounter 风格一致）

## 单元测试设计

### InMemoryTokenBlacklistTest

**形态**：class（JUnit 5），不引入 Spring 上下文
**包路径**：`com.aimedical.modules.commonmodule.auth.blacklist.InMemoryTokenBlacklistTest`
**注解**：无（package-private class，与 InMemoryRateLimitGuardTest / LoginAttemptTrackerTest / SlidingWindowCounterTest 风格一致）

**测试方法清单**（12 用例）：

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

**测试策略**：

1. **正常路径（用例 1/2/3/10）**：使用 `new InMemoryTokenBlacklist()` 默认实例，调用 add/isBlacklisted 验证基本契约。用例 3 验证不同 jti 互不干扰。用例 10 验证重复 add 同一 jti 的幂等性。

2. **过期清理（用例 4/5/11/12）**：使用 package-private 测试构造器 `new InMemoryTokenBlacklist(preFilledMap)` 注入预填数据，跳过 ScheduledExecutorService 创建：
   - 用例 4：注入包含过期 entry 的 map → 调用 `cleanup()` → 断言 `isBlacklisted` 返回 false
   - 用例 5：同 4，过期时间戳早于 now
   - 用例 11：通过反射 helper 读取 map size，确认 cleanup 后过期条目被移除
   - 用例 12：注入包含过期和未过期 entry 的 map → 调用 `cleanup()` → 通过反射断言过期条目被移除但未过期条目保留

3. **错误路径（用例 6/7）**：使用 `assertThrows(NullPointerException.class, () -> tracker.method(null))` 验证 null jti 一致抛 NPE。

4. **并发安全（用例 8/9）**：使用 `CountDownLatch` + `CyclicBarrier` 确保线程同时启动：
   - 用例 8：多线程并发放置同一 jti，主线程等待后通过反射读取 map size 为 1，断言 `isBlacklisted` 返回 true
   - 用例 9：多线程并发放置不同 jti，主线程等待后通过反射读取 map size 等于线程数，每个 jti 的 `isBlacklisted` 均返回 true

**反射访问方案**：
- 生产类字段保持 `private final`（与 SlidingWindowCounter / InMemoryRateLimitGuard 字段可见性约定一致）
- 测试类在同包内通过 `Field.setAccessible(true)` 访问 `private` 字段
- 反射 helper 集中封装在测试类的私有静态方法中，对外暴露 `readMapSize` 和 `readExpirationTime` 两个方法

**反射 helper 代码模板**：
```java
private static int readMapSize(InMemoryTokenBlacklist blacklist) throws Exception {
    Field f = InMemoryTokenBlacklist.class.getDeclaredField("blacklist");
    f.setAccessible(true);
    return ((ConcurrentHashMap<?, ?>) f.get(blacklist)).size();
}

private static long readExpirationTime(InMemoryTokenBlacklist blacklist, String jti) throws Exception {
    Field f = InMemoryTokenBlacklist.class.getDeclaredField("blacklist");
    f.setAccessible(true);
    ConcurrentHashMap<String, Long> map = (ConcurrentHashMap<String, Long>) f.get(blacklist);
    Long exp = map.get(jti);
    return exp != null ? exp : -1L;
}
```

**过期清理测试代码模板**：
```java
void shouldReturnFalseForExpiredEntry() {
    ConcurrentHashMap<String, Long> preFilled = new ConcurrentHashMap<>();
    preFilled.put("expired-jti", System.currentTimeMillis() - 100_000);
    preFilled.put("valid-jti", System.currentTimeMillis() + 100_000);
    InMemoryTokenBlacklist blacklist = new InMemoryTokenBlacklist(preFilled);
    assertTrue(blacklist.isBlacklisted("expired-jti"));
    blacklist.cleanup();
    assertFalse(blacklist.isBlacklisted("expired-jti"));
    assertTrue(blacklist.isBlacklisted("valid-jti"));
}
```

**并发测试代码模板**：
```java
void shouldHandleConcurrentAddSameJti() throws Exception {
    int threadCount = 10;
    InMemoryTokenBlacklist blacklist = new InMemoryTokenBlacklist();
    CountDownLatch latch = new CountDownLatch(threadCount);
    CyclicBarrier barrier = new CyclicBarrier(threadCount);

    for (int i = 0; i < threadCount; i++) {
        new Thread(() -> {
            try { barrier.await(); } catch (Exception e) { Thread.currentThread().interrupt(); }
            blacklist.add("same-jti", System.currentTimeMillis() + 60_000);
            latch.countDown();
        }).start();
    }
    assertTrue(latch.await(10, TimeUnit.SECONDS));
    assertTrue(blacklist.isBlacklisted("same-jti"));
    assertEquals(1, readMapSize(blacklist));
}
```

## 修订说明（v6 r1）

| 审查意见 | 修改措施 |
|---------|---------|
| Issue 1 [一般]：`cleanupExecutor` final 字段在双构造器场景下无法编译 | 采用方案 B——`cleanupExecutor` 改为公开构造器中的局部变量，不作为实例字段存储；测试构造器不存在 executor 初始化问题；字段表已注明不作为字段存储 |
| Issue 2 [轻微]：`cleanup()` 为 public 的理由不成立（SlidingWindowCounter 同用 `this::cleanup` 但为 private） | `cleanup()` 可见性改为 **package-private**，理由更正为"测试用例需直接调用以验证清理行为，同包访问无需 public" |
| Issue 3 [轻微]：字段表遗漏 `cleanupExecutor` | 字段表已补充说明 `cleanupExecutor` 不作为实例字段存储（方案 B 自然消除该字段的文档遗漏） |
