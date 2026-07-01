# 设计审查报告（v12 r1）

## 审查结果
REJECTED

## 发现

### [严重] PrescriptionAssistController 构造依赖不足以支持 getSuggestion 端点

**问题**：PrescriptionAssistController（第42行）的构造方式仅注入 `PrescriptionAssistService`，但 GET `/api/prescription/assist/suggestion/{taskId}` 端点的行为契约（第52行）需要查询 `SuggestionStore` 中的 `AiSuggestionResult`。`PrescriptionAssistService` 接口仅提供 `assist()` 和 `checkDose()` 两个方法（第199-200行），没有用于查询 suggestion 的方法。控制器缺少必要的依赖来执行 getSuggestion 逻辑。

**为什么是问题**：getSuggestion 端点需要按 taskId 访问 SuggestionStore 以判断 AiSuggestionResult 的四种状态（不存在/PENDING/COMPLETED/FAILED）。设计指定了完整的行为契约却未提供实现该行为的依赖路径，属于不可实现的缺陷。

**期望的修正方向**：在 PrescriptionAssistController 的构造依赖中增加 SuggestionStore，或在 PrescriptionAssistService 中增加 getSuggestion 方法供控制器调用。

### [一般] DosageThresholdService 剂量超限判定条件重叠

**问题**：第270-271行定义了超标程度判定规则——`> singleMax*2 → CRITICAL`、`> singleMax → WARNING`。这两个条件不互斥：当 `dosage > singleMax * 2` 时同时满足两个条件，但未指定优先级。

**为什么是问题**：实现者可能产生歧义——是先判断 CRITICAL 后短路，还是同时产出两个告警，还是其他逻辑。设计未给出明确的优先级约定，可能导致实现不一致。

**期望的修正方向**：明确指定优先级规则，例如"先判定 CRITICAL 条件（>singleMax*2），命中则直接返回 CRITICAL；未命中再判定 WARNING 条件"；或改用 `else if` 模式的文档化描述。

## 修改要求（仅 REJECTED 时）

### 严重问题 1 — PrescriptionAssistController 依赖不足
- **问题**：控制器仅注入 PrescriptionAssistService，不足以实现 getSuggestion 端点
- **期望方向**：增加 SuggestionStore 依赖或扩展 PrescriptionAssistService 接口

### 一般问题 1 — DosageThresholdService 超限条件优先级不明确
- **问题**：CRITICAL 与 WARNING 条件的判定互斥性未定义
- **期望方向**：明确 if/else if 的优先级顺序
