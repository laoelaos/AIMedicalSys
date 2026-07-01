# 设计审查报告（v28 r1）

## 审查结果
APPROVED

## 发现

### 审查范围
- 修复 `RegistrationEventListenerTest.shouldContainAllSevenFieldsInEventPayloadOnRecover` 因 ObjectMapper 缺少 JSR310 模块导致的 NPE
- 涉及 1 个文件：`RegistrationEventListenerTest.java`

### 逐项分析

**1. 根因定位 — 正确**
- 设计准确地指出 `setUp()` 中 `objectMapper = new ObjectMapper()` 未注册 `JavaTimeModule`，导致 `RegistrationEvent.eventTime`（`LocalDateTime`）序列化失败，触发 `recover()` catch 块回退 JSON，最终 `parsed.get("registrationId")` 为 null 引发 NPE。
- 与任务描述、代码分析一致。

**2. 修复方案 — 正确**
- 变更 1：添加 `import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;` — 包路径正确，依赖已在 classpath 上（`common` 模块的 `JacksonConfig` 已使用）。
- 变更 2：`setUp()` 中 `objectMapper.registerModule(new JavaTimeModule())` — 代码语义正确。

**3. 序列化格式考量 — 不影响通过**
- `JavaTimeModule` 在默认 `WRITE_DATES_AS_TIMESTAMPS=true` 下将 `LocalDateTime` 序列化为数组（如 `[2026,6,30,10,0]`），而非 ISO-8601 字符串。
- 但测试 L123 仅检查 `assertNotNull(parsed.get("eventTime"))`，不要求特定格式。序列化成功后该字段为 `ArrayList<Integer>`，非空，断言通过。
- 无需额外禁用 `WRITE_DATES_AS_TIMESTAMPS` 即可满足测试要求。

**4. 命名策略差异 — 不影响通过**
- 测试中的 ObjectMapper 使用默认 camelCase 属性名；生产代码通过 Spring `JacksonConfig` 配置了 SNAKE_CASE。
- 测试 L117–L123 使用 camelCase key 访问（如 `"registrationId"`），与测试用 ObjectMapper 一致。
- 测试验证的是 `recover()` 方法能否产出含全部 7 字段的 JSON 字符串，不关心生产环境的命名策略。

**5. 对其他测试的影响 — 无**
- `shouldWriteDeadLetterEventOnRecover`：检查 payload 非空，修复后依然满足。
- `shouldUseFallbackPayloadWhenSerializationFails`：使用独立 `failingMapper`，不受影响。
- `shouldSetFailReasonInDeadLetterEvent`：仅检查 failReason 文本，不受影响。
- 其余 handler 测试（L51–92）：不涉及 ObjectMapper，不受影响。

**6. 依赖可用性 — 已验证**
- `jackson-datatype-jsr310` 已通过 `common` 模块传递依赖引入，classpath 可用。

### 结论
无 [严重] 或 [一般] 发现。设计准确、完整，可直接进入编码阶段。
