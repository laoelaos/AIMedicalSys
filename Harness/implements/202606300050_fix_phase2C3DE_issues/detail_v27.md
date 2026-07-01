# 详细设计（v27）

## 概述

R26 生产代码正确。唯一阻断开是 `RegistrationEventListenerTest.java:130` 中 `throw new JsonProcessingException("Simulated failure")` 因 `JsonProcessingException(String)` 构造器为 `protected` 访问权限导致编译失败。本设计仅修复该编译错误。

## 文件规划

| 文件路径 | 操作 | 职责 |
|---------|------|------|
| `AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/RegistrationEventListenerTest.java` | 修改 | 替换 protected 构造器为 public 子类构造器 |

## 类型定义

无新增类型。

## 修改内容

### RegistrationEventListenerTest.java L129-131 及 import

**文件路径**：`.../consultation/RegistrationEventListenerTest.java`

**操作**：两处修改

**修改 1** — 新增 import：
- `import com.fasterxml.jackson.core.JsonParseException;`

**修改 2** — 替换 L130 抛出语句：
```
- throw new JsonProcessingException("Simulated failure");
+ throw new JsonParseException(null, "Simulated failure");
```

**保留不变**：
- L12 `import com.fasterxml.jackson.core.JsonProcessingException;` — 仍用于 L129 方法签名 `throws JsonProcessingException`
- L129 方法覆写签名 `public String writeValueAsString(Object value) throws JsonProcessingException` — 必须保持以匹配父类方法签名

## 错误处理

无变更。`JsonParseException` 是 `JsonProcessingException` 的直接 public 子类，生产代码 catch 块 `catch (JsonProcessingException e)` 完全兼容。

## 行为契约

- `JsonParseException(JsonLocation loc, String msg)` 构造器为 `public`，`null` 作为 JsonLocation 参数合法
- 异常类型仍为 `JsonProcessingException` 的子类型，测试目的不变（验证序列化异常时的降级行为）

## 依赖关系

- `com.fasterxml.jackson.core.JsonParseException` — 新增依赖（Jackson core 已有，仅需 import）

## 修订说明（v27 R1）

此为 v26（首轮）之后的 RETRY 轮次，唯一变更是测试端编译问题修复，不涉及生产代码设计变更。
