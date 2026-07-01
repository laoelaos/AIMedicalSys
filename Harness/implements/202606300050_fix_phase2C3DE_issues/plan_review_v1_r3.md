# 计划审查报告（v1 r3）

## 审查结果
REJECTED

## 发现

### [严重] C15/E01（P0）完全未纳入计划
- **问题**：计划声称覆盖诊断报告中列出的全部 54 项问题（P0—P2），但 **C15/E01「RegistrationEventListener @Retryable 对所有 Exception.class 重试」**（P0，必须立即修复）在 15 个轮次中均未出现。
- **证据**：`RegistrationEventListener.java:36` 使用 `@Retryable(retryFor = Exception.class, maxAttempts = 3, ...)` 捕获所有异常类型（含 IllegalArgumentException、NullPointerException 等不可治愈异常）。诊断报告第 42–46 行要求限制为 `{DataAccessException.class, TimeoutException.class}` 并增加 `noRetryFor={IllegalArgumentException.class, NullPointerException.class}`。OOD §3.1 同样要求"仅对 DataAccessException、TimeoutException 等可治愈临时异常触发重试"。
- **风险**：上线后若业务代码抛出 IllegalArgumentException 等不可重试异常，消费端将无意义重试 3 次后进入死信队列，浪费数据库连接和重试资源。所有 P0 必须纳入计划，此遗漏导致修复范围不完整。
- **修正方向**：新增一个轮次（或在 R2/R15 中补入）实现 C15/E01 修复，仅对可治愈异常启用重试。

### [严重] R7（A07/A11）先于 R12（A09）执行，违反 A09→A07→A11 修复顺序
- **问题**：计划中 **R7 包含 A07 + A11**，**R12 包含 A09**。但诊断报告「跨问题耦合副作用分析」§A07×A09×A11（第 401–409 行）明确要求修复实施顺序为：**(1) 先修复 A09** → (2) 再修复 A07 → (3) 最后修复 A11，且" A11 不可先于 A09 步骤(1)执行"。
- **原因**：A09 要求 PrescriptionAuditServiceImpl 在调用 `toAuditResponse()` 前检查 `aiResult.isSuccess() != true || aiResult.getData() == null` 并走降级路径。A07 要求 `AiResult.success(data)` 增加 `Objects.requireNonNull(data)` 断言。若在 A09 降级路径未就绪时先增加 A07 断言，业务层的 `&& getData() != null` 防御检查（A11）已被移除，PrescriptionAuditServiceImpl 将暴露 NPE 风险。
- **修正方向**：将 A09 调整至 R7（与 A07/A11 同轮且按 A09→A07→A11 顺序实施），或重新编排轮次使 R12（含 A09）先于 R7 执行。

### [轻微] 缺失 P2 可并行修复项（P09/P12/P15）
- P09（PrescriptionItem.unit OOD 对齐）、P12（DrugInteractionRule @ConditionalOnProperty）、P15（AuditResponse.fromFallback 序列化）未纳入任何轮次。鉴于三者均为 P2、且 P09 需要前置 OOD 文档修改，可视为合理剪裁。建议在计划中显式说明这三项被排期在外的原因，避免验收时被认为是遗漏。

### [轻微] 选择理由中的问题计数需核实
- 计划开头声称"54 项问题"，但实际未将 C15/E01 计入其中，且部分合并项（如 C06/E03 计为 2 项）的计数方式不透明。建议统一按诊断报告原始条目编号统计或明确合并计数规则。
