# OOD 设计方案质量审查报告（v9）

## 审查结论

**有条件通过**。产出在整体架构清晰度、核心抽象完备性、异常场景覆盖等方面达到较高成熟度。以下列出 4 项待修正问题，建议修正后定稿。

---

## 问题列表

### 问题 1：[重要] §3.7 PrescriptionLocalRuleFallback 引用 PrescriptionCheckRequest 上不存在的字段

**问题描述**：`PrescriptionLocalRuleFallback` 的过敏史检查项（§3.7 最小安全规则表第 4 行）声称从 `PrescriptionCheckRequest.allergyInfo` 获取过敏史数据。但 §3.11.2 定义的 `PrescriptionCheckRequest` 扩展字段为 `medications`、`patientInfo`、`diagnosisCodes` 三项，不存在 `allergyInfo` 直接字段。`patientInfo` 的描述中虽提到"含过敏史"，但 `PatientInfo` 类型本身在文档中未被正式定义（无字段表、无内嵌结构），且从 `patientInfo` 对象访问过敏信息的路径应为 `patientInfo.getAllergyInfo()` 而非 `request.allergyInfo`。

**所在位置**：§3.7 最小安全规则表（第 4 行，line 2160）

**该问题从 v5 版本起持续存在，历经 4 次迭代均未被检出修正**。

**严重程度**：重要

**改进建议**：方案 A（推荐）：将过敏史检查的数据来源改为 `request.patientInfo`，注明需从 `PatientInfo` 类型中提取过敏信息的方法签名（如 `patientInfo.getAllergyInfo(): List<String>`）。方案 B：若意图将 `allergyInfo` 独立作为 `PrescriptionCheckRequest` 的直接字段，则应在 §3.11.2 的 DTO 扩展字段定义中补充该字段，并同步修正 `PrescriptionAssistRequest`（§3.11.4 已定义 `allergyInfo: List<String>` 作为直接字段，引用方式一致）。

---

### 问题 2：[重要] §4.1 parsedResult 变量作用域错误的伪代码结构缺陷

**问题描述**：`doExecuteInternal()` 伪代码中，`parsedResult` 变量在嵌套 try-catch 块的内部定义（structuredChat 成功路径和 chat 回退成功路径各有一处赋值），但成功后的指标采集与返回代码（line 2906–2916）位于全部 try-catch 结构之外，直接引用 `parsedResult`。在真实 Java 代码中，try 块内定义的局部变量在 try 块外不可访问——此伪代码结构会直接导致编译错误。

**所在位置**：§4.1 `doExecuteInternal()` 伪代码（line 2809~2810、line 2863、line 2906~2916）

**严重程度**：重要

**改进建议**：在第一个 try 块之前声明 `parsedResult` 为方法级局部变量（`Object parsedResult = null`），两个成功路径仅对其赋值，line 2906 在访问前添加 null 守卫检查。此修正不会改变设计语义，但消除伪代码与实现之间的结构性鸿沟。

---

### 问题 3：[中等] §3.5 聚合 SQL 使用 MySQL 不支持的 PERCENTILE_CONT

**问题描述**：`AiCallLogStats` 的聚合 INSERT SQL 伪代码（line 2060–2062）使用了 `PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY elapsed_ms)`，此函数是 PostgreSQL/Oracle 的窗口函数，MySQL 8.0 原生不支持。文档声明数据库类型以 MySQL 方言表述（line 2011），却使用了非 MySQL 语法。

**该问题在 v4（a_v4_review_v1）和 v5 审查报告中均已被识别为"[轻微]"，但历经 4 次迭代仍未在文档中修正**。

**所在位置**：§3.5 聚合 SQL（line 2060~2062）

**严重程度**：中等

**改进建议**：将伪代码替换为 MySQL 兼容的百分位计算方式。MySQL 8.0 中可使用 `ROW_NUMBER()` + `COUNT(*)` 窗口函数方案或 `PERCENT_RANK()` 实现近似计算，或在伪代码中明确标注"此为示意 SQL，实现层需根据所选数据库替换为对应方言百分位函数"。建议方案：在伪代码后添加注释说明各数据库的替代函数（如 PostgreSQL 用 `PERCENTILE_CONT`、MySQL 用 `PERCENT_RANK()` + 应用层计算）。

---

### 问题 4：[中等] 多个 @Scheduled 任务缺少调度线程池配置定义

**问题描述**：文档中定义了 3 个使用 `@Scheduled` 的组件：(1) 指标清理（line 2027，每日凌晨），(2) CredentialProvider 退避定时器（line 1693，由 `@Scheduled` 或 `ScheduledExecutorService` 驱动），(3) ModelRouter 配置轮询（line 3121，`fixedDelay = 60000`）。但文档未定义调度任务的线程池配置。Spring 默认使用单线程的 `ThreadPoolTaskScheduler`（pool size = 1），指标清理的 DDL 操作（`DROP PARTITION`，持有 Metadata Lock）可能阻塞其他调度任务。

**所在位置**：§3.2（CredentialProvider）、§3.5（metrics cleanup）、§6.1（ModelRouter polling）、§3.9（AiPlatformConfig Bean 定义未含调度器）

**严重程度**：中等

**改进建议**：在 §3.9 `AiPlatformConfig` 中补充 `@Bean("scheduledTaskExecutor")` 定义，配置 `ThreadPoolTaskScheduler` 的 pool size（推荐至少 2~3，分离 DDL 清理与其他调度任务），或在文档中显式声明指标清理任务使用独立 `ScheduledExecutorService`（如 `credentialProviderBackoffScheduler` 已在 §3.2 提及，但 metrics 清理和 ModelRouter 轮询未指定）。

---

## 无显著问题的维度

- **需求响应充分度**：文档充分覆盖了 Phase 5 包 G 的全部 13 项 AI 能力的 OOD 设计要素（类图、核心职责、协作关系、关键接口、状态模型），设计风格与 Phase0/Phase1ABD 保持一致。
- **事实准确性（非问题项）**：经与代码库抽查比对，文档对现有 DTO 状态的描述（`TriageRequest` 仅含 `chiefComplaint`、`TriageResponse` 含 `recommendedDepartments` 和 `reason`、`DiagnosisRequest` 为空类`、`DegradationStrategy` 无 `getOrder()`、`DegradationContext` 为空骨架、`DegradationReason` 不存在等）均与实际代码一致。
- **降级/熔断/超时体系**：逻辑严密，三层超时（管线 orTimeout → structuredChat 60% → chat 回退 40%）拆分合理，熔断器与端点健康管理的统一探测机制完整。
- **实施引导**：§1.6 API Surface 状态表和 §1.7 实施拓扑顺序为编码实施提供了清晰的起点和优先级排序。
