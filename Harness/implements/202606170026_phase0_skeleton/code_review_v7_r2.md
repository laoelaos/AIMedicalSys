# 代码审查报告（v7 r2）

## 审查结果
REJECTED

## 发现

- **[一般]** `FallbackAiService.java:70-184` — 13 个方法的委托分发均使用 `.join()` 阻塞等待委托的 `CompletableFuture`。AiService 接口全部返回 `CompletableFuture`，其设计意图是异步非阻塞。FallbackAiService 作为装饰器，应通过 `thenApply()`/`thenCompose()` 组合 future，而非用 `.join()` 转同步阻塞。这破坏了异步契约，且在高并发场景下可能导致线程饥饿。

- **[轻微]** `MockAiServiceTest.java:36-180` — 测试方法名使用 `ShouldReturnSuccessResult` 后缀，与详细设计 `detail_v7.md` 中指定的 `ShouldReturnMockData` 后缀不一致。

- **[轻微]** `FallbackAiServiceTest.java:35` — 测试方法 `shouldReturnDegradedWhenNoDelegatesAvailable` 名称与详细设计 `detail_v7.md` 中指定的 `shouldReturnFallbackResultWhenNoDelegateAvailable` 不一致。

- **[轻微]** `FallbackAiService.java:49` — `firstEmptyDelegateCall` 标志位非线程安全。并发调用下多个线程可能同时判断为首次，导致输出多条 ERROR 日志而非设计要求的"首次 ERROR、后续 WARN"。

## 修改要求（仅 REJECTED 时）

### 1. **[一般]** FallbackAiService.java — 委托分发阻塞问题

**位置**：`FallbackAiService.java` 第 70-184 行（全部 13 个方法）

**问题**：每个方法中用 `.join()` 阻塞等待 delegate 的 `CompletableFuture`，将异步调用转为同步阻塞。

**期望方向**：将模式改为 future 组合，例如：
```java
return delegates.get(0).triage(request)
    .thenApply(this::applyStrategies);
```
同时将 `applyStrategies` 的返回类型从 `CompletableFuture<AiResult<T>>` 改为 `AiResult<T>`（当前是 `CompletableFuture` 但实际只是 `completedFuture` 包装），或保留 current 签名但移除冗余包装。

### 2. **[轻微]** MockAiServiceTest.java — 方法命名对齐设计

**位置**：第 51、62、73、83、94、105、117、127、138、149、160、171 行

**问题**：方法名后缀为 `ShouldReturnSuccessResult`，设计与 `detail_v7.md` 中 `ShouldReturnMockData` 不符。

**期望方向**：统一重命名为 `xxxShouldReturnMockData`。

### 3. **[轻微]** FallbackAiServiceTest.java — 方法命名对齐设计

**位置**：第 35 行

**问题**：`shouldReturnDegradedWhenNoDelegatesAvailable` 与设计的 `shouldReturnFallbackResultWhenNoDelegateAvailable` 不一致。

**期望方向**：按设计重命名。

### 4. **[轻微]** FallbackAiService.java — 线程安全

**位置**：第 49 行

**问题**：`firstEmptyDelegateCall` 在并发下可能重复输出 ERROR 日志。

**期望方向**：使用 `AtomicBoolean` 替代 `boolean` 保证可见性和原子性。
