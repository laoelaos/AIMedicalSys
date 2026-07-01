# 任务指令（v28）

## 动作
RETRY

## 任务描述
修复 `RegistrationEventListenerTest.shouldContainAllSevenFieldsInEventPayloadOnRecover` 运行时失败的根因：test `setUp()` 中 `objectMapper = new ObjectMapper()` 缺少 JSR310 模块，导致 `RegistrationEvent.eventTime`（`LocalDateTime` 类型）序列化失败，触发 `recover()` catch 块的回退 JSON（仅含 `sessionId`），测试 L117 `parsed.get("registrationId")` 返回 null 引发 NPE。

涉及文件：`AIMedical/backend/modules/consultation/src/test/java/com/aimedical/modules/consultation/RegistrationEventListenerTest.java`

## 选择理由
R26/R27 生产代码正确（DeadLetterCompensationService 的 EXPIRED 迁移、RegistrationEventListener 完整 7 字段 JSON 序列化均已正确实现）。唯一阻断开是测试中 ObjectMapper 缺少 JSR310 模块。修复后验证通过即本轮次所有计划任务完成。

## 任务上下文
### 失败原因
- 文件：`RegistrationEventListenerTest.java`（方法 `shouldContainAllSevenFieldsInEventPayloadOnRecover`）
- 错误：NullPointerException at L117 — `((Number) parsed.get("registrationId")).longValue()` 中 `parsed.get("registrationId")` 为 null
- 根因：`setUp()` L47 中 `objectMapper = new ObjectMapper()` 未注册 `JavaTimeModule`。Jackson 默认不支持 `LocalDateTime` 序列化，导致 `writeValueAsString(event)` 抛出 `JsonProcessingException`，`recover()` catch 块捕获后写入仅含 `sessionId` 的回退 JSON 字符串，因此反序列化后的 Map 中不含 `registrationId` 等字段

### 修复方案
在 `setUp()` 方法中为 `objectMapper` 注册 `JavaTimeModule`：

```java
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
...
objectMapper = new ObjectMapper();
objectMapper.registerModule(new JavaTimeModule());
```

说明：
- `JavaTimeModule` 已在 `common` 模块的 `JacksonConfig` 中使用，classpath 上可用（common 为 consultation 的传递依赖）
- 注册后 `LocalDateTime` 可正常序列化为 ISO-8601 字符串，`writeValueAsString(event)` 将生成包含全部 7 字段的 JSON

### 验证方式
```bash
mvn test -pl modules/consultation -Dtest=RegistrationEventListenerTest -am
```

## 已有代码上下文
- 设计：`detail_v26.md` — 生产代码无问题
- 实现：`code_v26.md` + `code_v27.md` — 生产代码正确，测试端修复遗漏
- 测试：`test_v27.md`（不存在）— 无额外测试新增
- 验证：`verify_v27.md` — FAILED（consultation 1 error）

## RETRY 说明
- R26（首轮 C14+E05）：生产代码正确；测试 `JsonProcessingException` protected 构造器导致编译失败
- R27（RETRY 编译修复）：编译通过；运行时 `ObjectMapper` 缺少 `JavaTimeModule` 导致 `LocalDateTime` 序列化异常触发回退路径
- 本次（R28 RETRY）：修复测试 ObjectMapper 注册 `JavaTimeModule`，使 `RegistrationEvent` 完整 JSON 序列化正常工作
