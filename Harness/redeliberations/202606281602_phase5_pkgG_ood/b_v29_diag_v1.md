# 质量审查报告 — Phase 5 包 G OOD 设计文档（v29_copy_from_v28）

## 审查范围

- **待审查产出**：`a_v29_copy_from_v28.md`（Phase 5 包 G AI 进阶底座架构级 OOD 设计方案）
- **用户需求**：完成 Phase 5 包 G 的完整 OOD 设计，覆盖类图、核心职责、协作关系、关键接口、状态模型等要素，保持与 Phase0/Phase1ABD 的风格一致性
- **审查维度**：需求响应充分度、整体深度和完整性、实际落地可行性（侧重内部审议未充分覆盖的维度）
- **已知上下文**：该产出已完成组件 A 内部审议（设计-验证循环），已覆盖技术可行性等内部维度

---

## 发现的问题

### 问题 1：[严重] 文档版本号标题与实际状态不一致

- **问题描述**：文档第 1 行标题声明为 `（v27）`，但文件名命名为 `a_v29_copy_from_v28.md`，且正文末尾包含「修订说明（v28）」和「修订说明（v29）」两个修订说明表。标题版本号（v27）落后于文档实际状态（v29）。读者通过标题判断版本时会产生严重误导——以为是 v27 版本，实际是 v29 版本（含 v28/v29 的修订）。
- **所在位置**：第 1 行；第 4710-4731 行（v28/v29 修订说明）
- **严重程度**：严重
- **改进建议**：将第 1 行标题中的 `（v27）` 更新为 `（v29）`，使标题与实际版本一致。同步检查文档头部变更摘要中版本范围的描述（当前写 `v7~v17`，需确认是否需更新）。

### 问题 2：[严重] 文档头部声明与正文内容矛盾

- **问题描述**：文档第 3 行的变更摘要中声明："尾部仅保留 v27 修订说明作为最新变更变更追踪参考"。但文档末尾实际包含的是 v27、v28、v29 共三个修订说明表，并非"仅保留 v27"。此声明与事实严重不符。
- **所在位置**：第 3 行（声明）；第 4692-4731 行（v27/v28/v29 修订说明）
- **严重程度**：严重
- **改进建议**：修正头部声明为"尾部保留 v27~v29 修订说明作为最新变更追踪参考"，或将两个声明统一。确保读者通过头部声明能准确预判尾部内容。

### 问题 3：[重要] §4.2 薄适配器超时日志使用了错误的变量

- **问题描述**：薄适配器 `doExecuteInternal()` 伪代码中，委托调用使用 `effectiveThinAdapterTimeout`（支持 per-capability 覆盖）作为实际超时值，但超时降级的 WARN 日志却使用了 `thinAdapterTimeout`（全局默认值）记录超时阈值。当 per-capability 超时覆盖生效时（如 IMAGE_ANALYSIS 的 45s），日志将显示默认值 30s 而非实际生效的 45s，严重误导运维排障。
- **所在位置**：§4.2 薄适配器特化管线伪代码，第 3927 行（实际使用 `effectiveThinAdapterTimeout`）vs 第 3934 行（日志使用 `thinAdapterTimeout`）
- **严重程度**：重要
- **改进建议**：将第 3934 行的 `thinAdapterTimeout.toMillis()` 替换为 `effectiveThinAdapterTimeout.toMillis()`，使日志反映实际生效的超时值。

### 问题 4：[重要] v27 修订说明声称的修复未在伪代码中落地

- **问题描述**：v27 修订说明第 9 条（第 4704 行）明确要求将字符估算回退分支的触发阈值从 3000 提升至 4000，并移除了 `estimatedTokens = 2000` 硬编码跳跃值。但 §4.1 `DiscussionConclusionCapabilityExecutor` 特化伪代码中（第 3846 行），阈值判断 `if estimatedTokens > 3000:` 仍然适用于估算路径。这意味着 v27 声称的修复（估算路径阈值提高到 4000）未在伪代码中实际执行——伪代码中估算路径和精确路径共用同一个 3000 阈值判断。
- **所在位置**：§4.1 `DiscussionConclusionCapabilityExecutor.doExecuteInternal()` 特化伪代码，第 3846 行；v27 修订说明第 9 条（第 4704 行）
- **严重程度**：重要
- **改进建议**：将第 3846 行的阈值判断改为区分两路径——精确路径使用 3000、估算路径使用 4000（或在估算分支中将 `estimatedTokens` 按比例缩小后再与 3000 比较），确保伪代码行为与修订说明一致。

### 问题 5：[一般] AiCallRecord.capabilityName 字段填充来源未定义

- **问题描述**：§3.5 `AiCallRecord` 字段定义表（第 2207-2208 行）列出了 `capabilityName: String` 字段，但所有三个工厂方法签名（`success()`/`failure()`/`degraded()`）的入参中均不包含 `capabilityName` 参数。管线伪代码中调用 `AiCallRecord.success()` 等工厂方法时也不传入 `capabilityName`。实现者无法从设计文档中获知 `capabilityName` 的填充来源——是通过 `capabilityId` 内部映射自动补全，还是独立传入，或从其他上下文推导。
- **所在位置**：§3.5 `AiCallRecord` 字段定义表（第 2207-2208 行）；工厂方法签名（第 2232-2252 行）；§4.1 管线伪代码中工厂方法调用点（第 3710-3715 行、第 3955-3956 行等）
- **严重程度**：一般
- **改进建议**：方案 A（推荐）：在 `AiCallRecord` 工厂方法签名中补充 `String capabilityName` 参数；方案 B：在字段定义表注释中说明 `capabilityName` 由工厂方法内部通过 `capabilityId` 从配置映射表自动补全。无论哪种方案，需在工厂方法 Javadoc 中显式说明填充规则。

### 问题 6：[一般] §3.11.4 PrescriptionAssist 模板变量与 DTO 结构潜在不匹配

- **问题描述**：`PrescriptionAssistCapabilityExecutor` 的 Prompt 模板变量包含 `{{patientAge}}`、`{{patientWeight}}`、`{{allergyInfo}}` 等扁平字段名（第 3145 行），但输入 DTO `PrescriptionAssistRequest` 将患者信息封装为 `PatientInfo` 内嵌值对象（`patientInfo: PatientInfo`）。该能力使用"方式 A（默认 `ObjectMapper.convertValue`）"提取模板变量（第 3147 行），Jackson 默认的 `convertValue` 对嵌套对象产生的是嵌套 key（如 `patientInfo.age`）而非扁平 key（如 `patientAge`），导致模板变量可能无法正确填充。
- **所在位置**：§3.11.4，第 3143-3147 行
- **严重程度**：一般
- **改进建议**：方案 A：将变量提取策略从方式 A 改为方式 B（自定义），手工将 `PatientInfo` 展开为扁平变量；方案 B：保持方式 A 但修改模板变量名为嵌套格式（如 `{{patientInfo.age}}`），或为 `PrescriptionCheckRequest` 和 `PrescriptionAssistRequest` 统一变量命名风格（两者均使用 `PatientInfo` 对象）。

### 问题 7：[一般] DegradationReason 枚举定义中 `STRATEGY_TRIGGERED` 拼接策略类名的格式未在 pseudo 全文中一致应用

- **问题描述**：§3.8 `DegradationReason` 枚举定义（第 2738 行）规定 `STRATEGY_TRIGGERED` 的拼接格式为 `DegradationReason.STRATEGY_TRIGGERED + ":" + strategy.getClass().getSimpleName()`，但 §4.1 降级预检路径中（第 3455 行）实际拼接的是 `DegradationReason.STRATEGY_TRIGGERED + ":" + strategy.getClass().getSimpleName()`，两者不一致——枚举常量为 `STRATEGY_TRIGGERED`（code 值 `"StrategyTriggered"`），而伪代码拼接字符串使用了 `"StrategyTriggered"` 作为常量名称而非 code 值语义。实际上伪代码引用的 `DegradationReason.STRATEGY_TRIGGERED` 是 Java 枚举常量引用，其 `.toString()` 或对应 code 值通常是 `"STRATEGY_TRIGGERED"`（常量名）而非 `"StrategyTriggered"`。降级原因的实际格式取决于枚举常量如何映射到 code 值（通过字段而非枚举名）。若枚举实现中 code 值与常量名不一致（第 2732-2739 行显示 code 使用大驼峰如 `"StrategyTriggered"`），则伪代码中 `+ ":" + strategy.getClass().getSimpleName()` 拼接后果缀正确但前段可能使用了枚举名而非 code 值。
- **所在位置**：§3.8 `DegradationReason` 枚举定义（第 2738 行）；§4.1 第 3455 行
- **严重程度**：一般
- **改进建议**：在 `DegradationReason` 枚举中明确定义 `getCodeValue()` 方法，伪代码中统一通过 `DegradationReason.STRATEGY_TRIGGERED.getCodeValue()` 获取 code 值而非依赖 `toString()` 或常量名到 code 值的隐式映射。所有降级路径中的枚举引用均按此方式修正。

---

## 整体质量评价

文档经过 29 轮迭代审查后，绝大多数严重和重要问题已得到修正，整体质量较高。类图、伪代码、YAML 配置、决策表、错误分类、测试策略等 OOD 核心要素覆盖完整，需求响应充分（13 项能力的迁移路径/管线设计/DTO 改造计划均有明确描述）。

当前遗留问题主要集中在**声明-事实一致性**（版本号、头部声明、修订说明落地）和**伪代码细节正确性**（日志变量、阈值逻辑）两个方面。这两个方面属于迭代审查中容易因关注宏观结构而忽略的细节，建议在 v30 中集中修正。

---
DIAG_WRITTEN:C:\Develop\Software\AIMedicalSys\Harness\redeliberations\202606281602_phase5_pkgG_ood\b_v29_diag_v1.md
主Agent请勿阅读产出文件内容，直接将路径转发给相关方。
