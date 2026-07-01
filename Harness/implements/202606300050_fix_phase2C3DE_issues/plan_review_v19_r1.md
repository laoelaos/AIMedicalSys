# 计划审查报告（v19 r1）

## 审查结果
REJECTED

## 发现

### 1. [一般] FallbackAiService 委托选择逻辑重构位置描述与实际代码不匹配
**R19 §4 (A06)** 以及 **task_v19 §4** 描述为 "FallbackAiService.applyStrategies()：当前仅 delegates.get(0)，改为遍历 delegates 列表，依次尝试 delegate"，但实际代码中：

- `applyStrategies()` 方法（L183-194）是后置处理函数，签名 `AiResult<T> applyStrategies(AiResult<T>)`，已经对所有 strategies 做迭代降级判断，且逻辑正确。
- 真正固定使用 `delegates.get(0)` 的是每个具体的 AiService 方法（triage/diagnosis/...共 13 个方法），例如 L71 `return delegates.get(0).triage(request).thenApply(this::applyStrategies)`。
- 任务描述的逻辑（对每个 delegate 预检查 shouldDegrade，跳过降级的 delegate，使用第一个通过的）无法在 `applyStrategies()` 内实现——因为此时 delegate 已经调用完毕，传入的是结果而非 delegate 本身。
- **影响**：实施者可能直接修改 `applyStrategies()` 而遗漏 13 个具体方法的 refactor，导致"遍历 delegates 依次尝试"的设计目标未实现。
- **修正方向**：将重构范围描述从 `applyStrategies()` 改为每个具体方法级别的 delegate 选择逻辑。建议新增私有的 `selectDelegate(DegradationContext)` 辅助方法，集中封装"遍历 delegates → 对每个 delegate 执行 strategy 预检查 → 返回第一个通过检查的 delegate"逻辑，各方法统一调用它替换 `delegates.get(0)`。

### 2. [轻微] PrescriptionAssistServiceImpl future.get() 行号偏差
**task_v19 §1** 写 "PrescriptionAssistServiceImpl.java ~L78"，但实际代码中 AI 调用在 L91 `aiService.prescriptionAssist(aiRequest).get()`。~L78 为空行（构造器闭括号后）。
- **影响**：极小，仅引用不精确。
- **修正方向**：行号改为 L91，或描述改为 "prescriptionAssist() 方法内"。

### 3. [轻微] A01 覆盖的 3 个 Service 中仅 TriageServiceImpl 有实际迁移内容
**task_v19 §5 (A01)** 列举 TriageServiceImpl/PrescriptionAuditServiceImpl/PrescriptionAssistServiceImpl 需要迁移至 AiResultFactory，但实际 grep 确认：
- TriageServiceImpl.java: L178/L180 有 2 处 `AiResult.degraded()` 需替换
- PrescriptionAuditServiceImpl.java: 没有 `AiResult.success/failure/degraded()` 静态调用
- PrescriptionAssistServiceImpl.java: 没有 `AiResult.success/failure/degraded()` 静态调用
- 后两个 Service 仅需补充 `import AiResultFactory`（如需），无实际替换内容。
- **影响**：不会导致编码错误，但任务描述与实际 scope 有出入。
- **修正方向**：标注仅 TriageServiceImpl 含实际替换内容，其他 Service 注明"确认无迁移内容"。
