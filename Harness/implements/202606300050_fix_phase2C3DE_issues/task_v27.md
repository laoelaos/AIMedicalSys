# 任务指令（v27）

## 动作
RETRY

## 任务描述
修复 R26 编译失败：RegistrationEventListenerTest.java:130 中 `throw new JsonProcessingException("Simulated failure")` 无法编译，因 `JsonProcessingException(String)` 构造器为 `protected` 访问权限。

涉及文件：`AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/RegistrationEventListenerTest.java`

## 选择理由
R26 生产代码正确（DeadLetterCompensationService 已实现 retryCount >= maxRetryCount 的 EXPIRED 迁移；RegistrationEventListener 已实现完整 RegistrationEvent 7 字段序列化）。唯一阻断开是测试编译错误。修复后全量构建通过即为本轮次所有计划任务完成。

## 任务上下文
### 失败原因
- 文件：`RegistrationEventListenerTest.java:130`（方法 `shouldUseFallbackPayloadWhenSerializationFails`）
- 错误：`JsonProcessingException(String)` 构造器在 Jackson 中为 `protected` 访问权限，测试匿名子类中无法直接调用
- 影响：consultation 模块 testCompile 失败（1 error），阻断全量构建

### 修复方案（推荐）
将 L129-131：
```java
throw new JsonProcessingException("Simulated failure");
```
替换为：
```java
throw new JsonParseException(null, "Simulated failure");
```
理由：
- `JsonParseException` 是 `JsonProcessingException` 的 public 子类
- `JsonParseException(JsonLocation loc, String msg)` 构造器为 `public`
- `null` 作为 `JsonLocation` 参数合法（Jackson 允许）
- 异常类型仍为 `JsonProcessingException`，生产代码 catch 块兼容

同时添加 import：`import com.fasterxml.jackson.core.JsonParseException;`

## 已有代码上下文
- 设计：`detail_v26.md`
- 实现：`code_v26.md` — 生产代码无问题
- 测试：`test_v26.md` — 仅测试端编译错误
- 验证：`verify_v26.md` — FAILED at consultation testCompile

## RETRY 说明
R26 实现的生产代码正确（DeadLetterCompensationService 的补偿前 retryCount 检查和 catch 块二次检查均正确实现 EXPIRED 迁移；RegistrationEventListener 已完成完整 7 字段序列化）。唯一失败原因是测试文件中构造 `JsonProcessingException` 时使用了 protected 构造器。
