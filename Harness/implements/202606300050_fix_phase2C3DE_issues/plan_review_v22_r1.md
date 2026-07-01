# 计划审查报告（v22 r1）

## 审查结果
REJECTED

## 发现

### **[严重]** DedupTaskScheduler.schedule() 无法获取 PrescriptionAssistRequest 数据，无法实现异步 AI 调用

**问题**：task_v22.md 要求 DedupTaskScheduler.schedule() 在创建 PENDING 条目后异步调用 `AiService.prescriptionAssist()`，但 `schedule()` 仅接收 `String prescriptionId`，而 `AiService.prescriptionAssist()` 需要完整的 `PrescriptionAssistRequest`（含 diagnosis、examResults、existingPrescription、patientInfo 等字段）。

**证据**：
- `DedupTaskScheduler.schedule(String prescriptionId)` 当前签名（DedupTaskScheduler.java:21）仅接收 prescriptionId
- `AiService.prescriptionAssist(PrescriptionAssistRequest)`（AiService.java:52）需要完整 request 对象
- `AssistConverter.toAiPrescriptionAssistRequest()`（AssistConverter.java:23）需要业务层 `PrescriptionAssistRequest` 作为入参
- `DedupTaskScheduler` 无任何渠道从 prescriptionId 重建出完整的 `PrescriptionAssistRequest`（`PrescriptionDraftContext` 仅存储 CRITICAL 告警，不含病史、检查结果、现有处方等字段）
- task_v22.md 明确要求"schedule() 方法签名保持不变（String schedule(String prescriptionId)）"（第 18 行），排除了签名变更的可能
- 被 `schedule()` 调用的入口 `PrescriptionAssistServiceImpl.checkDose()`（L180-210）仅接收 `DosageCheckRequest`（剂量检查参数），无完整处方上下文

**影响**：实施者将无法完成任务"从 schedule() 异步调用 AiService.prescriptionAssist()"。必须重新设计数据流——要么变更 schedule() 签名以接收完整 request，要么改为在 PrescriptionAssistServiceImpl.assist()（已有完整 request）中触发异步调用，要么新增数据存储使 schedule() 可凭 prescriptionId 检索完整 request。

### **[严重]** DedupTaskSchedulerTest 因构造函数变更将编译失败

**问题**：DedupTaskScheduler 构造函数将增加 AiService、AssistConverter、ObjectMapper 等参数，但 plan.md 和 task_v22.md 均未提及需要同步更新 `DedupTaskSchedulerTest`。

**证据**：
- `DedupTaskSchedulerTest.setUp()`（DedupTaskSchedulerTest.java:28）直接调用 `new DedupTaskScheduler(suggestionStore)`
- 当前构造器（DedupTaskScheduler.java:17）仅有 `SuggestionStore` 单参数
- 增加参数后测试编译必然失败

**影响**：prescription 模块 test compile 失败，需额外 RETRY 轮次修复。

### **[一般]** ObjectMapper 依赖未在主任务内容中明确列出

**问题**：task_v22.md 实施要点第 5 条要求 COMPLETED 时使用 `objectMapper.writeValueAsString(aiResult.getData())` 序列化 suggestion，但主任务描述"注入 AiService 和 AssistConverter"未包含 ObjectMapper。DedupTaskScheduler 当前无 ObjectMapper 字段，需补充注入或自行创建实例。

### **[轻微]** 文件数量不一致

**问题**：plan.md 路线表预计 3-5 个文件，task_v22.md "涉及文件"仅列出 3 个（其中 `AiSuggestionResult.java` 和 `PrescriptionAssistServiceImpl.java` 明确标注无需修改），实际仅 `DedupTaskScheduler.java` 1 个生产文件需修改。

## 修改要求

### [严重] 问题 1 — 异步 AI 调用数据源缺失
- **问题**：`schedule()` 无渠道获取 AI 调用所需的完整 `PrescriptionAssistRequest`
- **期望修正**：以下方案任选其一——
  (a) 变更 `schedule()` 签名以接收 `PrescriptionAssistRequest`（或携带所需数据的 context 对象），同步更新调用方 `checkDose()`；或
  (b) 不在 `DedupTaskScheduler` 中发起 AI 调用，改为在 `PrescriptionAssistServiceImpl.assist()`（已有完整 request）中触发异步 AI 调度；或
  (c) 新增中间数据存储（如 `PrescriptionAssistRequest` 暂存服务），使 `schedule()` 可凭 prescriptionId 检索完整请求。
- 完成后同步更新 plan.md 路线表和 task_v22.md 任务描述

### [严重] 问题 2 — DedupTaskSchedulerTest 编译兼容
- **问题**：构造函数新增参数后测试编译失败
- **期望修正**：在「涉及文件」中补充 `DedupTaskSchedulerTest.java`，明确要求同步更新 `setUp()` 中的构造调用（mock AI 依赖 + 传入 mock 实例）

### [一般] 问题 3 — ObjectMapper 依赖未明确
- **问题**：实施要点 5 与主注入描述不一致
- **期望修正**：在主任务描述（注入列表）中补充 `ObjectMapper`，或澄清替代方案（如 DedupTaskScheduler 内部创建 `new ObjectMapper()` 实例但需说明理由）
