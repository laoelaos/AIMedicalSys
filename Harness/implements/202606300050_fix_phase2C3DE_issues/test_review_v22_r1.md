# 测试审查报告（v22 r1）

## 审查结果
REJECTED

## 发现

- **[一般]** `PrescriptionAssistServiceImplTest.java` — 缺少对异步 AI 返回 `isSuccess()=false` 或 `getData()=null` 路径的测试。实现中（L363-370）明确处理了该分支，无测试覆盖意味着回归风险不可控。

- **[一般]** `PrescriptionAssistServiceImplTest.java` — 缺少对异步管线中 `InterruptedException` 处理的测试。实现中（L374-377）恢复中断标记并映射 FAILED，设计明确要求该行为，但无测试验证。

- **[一般]** `PrescriptionAssistServiceImplTest.java` — 缺少对 `objectMapper.writeValueAsString()` 抛出异常场景的测试。设计文档（第97行）和实现中（L364 可能抛出）均覆盖此路径，测试未覆盖。

- **[轻微]** `PrescriptionAssistServiceImplTest.java` — 5 个异步测试（L558/588/627/665/704）依赖 `Thread.sleep(300)` 作为异步同步手段，存在 CI 环境下的潜在脆性。建议考虑 `Awaitility` 或可控 `CompletableFuture` 注入。

## 修改要求（仅 REJECTED 时）

1. **文件**：`PrescriptionAssistServiceImplTest.java`
   - **位置**：在 async pipeline 测试组内（L474 附近），新增测试方法。
   - **问题**：缺少对 `aiResult.isSuccess()=false` 或 `aiResult.getData()=null` 时异步存储 FAILED 的测试。
   - **原因**：实现中存在 `else { status=FAILED; failReason="AI result not successful or data is null" }` 分支，未被测试覆盖，任何对此逻辑的无意修改都将无法被发现。
   - **期望**：新增测试，mock `aiService.prescriptionAssist()` 返回 `AiResult.success(null)` 或 `AiResult.failed(...)` 的异步结果，验证 `suggestionStore.put()` 写入的 result 包含 `FAILED` 状态。

2. **文件**：`PrescriptionAssistServiceImplTest.java`
   - **位置**：在 async pipeline 测试组内，新增测试方法。
   - **问题**：缺少对异步管线中 `InterruptedException` 的测试。
   - **原因**：实现中（L374-377）显式捕获 `InterruptedException` 并执行 `Thread.currentThread().interrupt()`。设计文档第96行也明确了该要求。无测试意味着中断恢复行为无法被验证。
   - **期望**：新增测试，mock 异步 future 以 `InterruptedException` 完成，验证 `suggestionStore.put()` 写入 FAILED，且可通过 `Thread.interrupted()` 确认中断标记已被恢复（或使用 spy 验证）。

3. **文件**：`PrescriptionAssistServiceImplTest.java`
   - **位置**：在 async pipeline 测试组内，新增测试方法。
   - **问题**：缺少对 `objectMapper.writeValueAsString()` 序列化失败的测试。
   - **原因**：设计文档第97行和实现中（L364）使用 `objectMapper.writeValueAsString()`，当序列化非可序列化对象时可能抛出异常。该异常应被外部 try-catch（L378-380）捕获并映射为 FAILED。
   - **期望**：新增测试，mock 异步 AI 返回成功结果但其 `getData()` 包含无法序列化的对象（如自引用循环使 Jackson 抛出 `JsonProcessingException`），验证 FAILED 状态。
