# 设计审查报告（v3 r2）

## 审查结果
REJECTED

## 发现

### [严重] SessionStore.put 返回 void，createSession 代码无法编译

设计第 259 行：
```java
DialogueSession existing = sessionStore.put(sessionId, session);
if (existing != null) { ... }
```

但 `com.aimedical.modules.commonmodule.store.SessionStore` 接口的 `put` 方法签名为 `void put(K key, V value)` —— 返回 `void`，而非 `V`。上述代码无法通过编译。

**根因**：设计者假设 `SessionStore` 遵循 `java.util.Map.put(K, V)` 语义（返回旧值），但该接口是自定义接口，`put` 无返回值。

**修正方向**：需重新设计 `createSession` 的并发安全方案。可行选项（至少选一）：
- 方案 A（推荐）：在 `DialogueSessionManager.createSession` 方法上添加 `synchronized` 关键字，配合 `containsKey` + `put` 组合检查，或直接 `put`（因已加 synchronized，无需 putIfAbsent）。
- 方案 B：扩展 `SessionStore` 接口增加 `V putIfAbsent(K key, V value)` 方法，并在实现类中提供原子语义。

### [严重] TransactionTemplate 不是自动配置的 Bean，构造器注入将失败

设计第 355 行声称：
> TransactionTemplate（Spring Boot 自动配置 Bean）

**此断言不成立**：Spring Boot `TransactionAutoConfiguration` 仅自动配置 `PlatformTransactionManager` Bean，**不**自动配置 `TransactionTemplate` Bean。代码库中也没有任何 `@Bean TransactionTemplate` 定义（已全局搜索确认）。构造器注入 `TransactionTemplate` 将在启动时抛出 `NoSuchBeanDefinitionException`。

**修正方向**：
- 方案 A（推荐）：回退到 v2 设计的正确做法 —— 注入 `PlatformTransactionManager`，在构造器内执行 `this.transactionTemplate = new TransactionTemplate(transactionManager);`。
- 方案 B：在配置类中显式声明 `@Bean public TransactionTemplate transactionTemplate(PlatformTransactionManager tm) { return new TransactionTemplate(tm); }`，然后构造器注入。

### [轻微] triage() 中 additionalResponses null 检查成为死代码

设计第 345 行已承认此问题。`additionalResponses` 改为声明时初始化 `new CopyOnWriteArrayList<>()` 后，`triage()` 中 `session.getAdditionalResponses() == null` 永远为 false。无运行时影响，可接受。

## 修改要求

1. **`DialogueSessionManager.createSession` 并发安全方案**：必须基于 `SessionStore` 接口的实际签名（`void put`）重新设计，当前设计代码无法编译。
2. **`TriageServiceImpl` TransactionTemplate 注入方式**：必须改用注入 `PlatformTransactionManager` + 构造器 `new TransactionTemplate()`，或先声明 `@Bean` 再注入。不可假设 Spring Boot 自动配置。
