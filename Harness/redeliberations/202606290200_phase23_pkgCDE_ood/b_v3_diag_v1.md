# 质量审查报告 — OOD 设计文档 v3

**审查对象**: `a_v3_copy_from_v2.md`
**审查轮次**: 第 3 轮
**审查视角**: 需求响应充分度、事实/逻辑正确性、深度与完整性（侧重内部审议未充分覆盖维度）
**审查时间**: 2026-06-29

---

## 1. 需求响应充分度

### 1.1 [严重] "规避 Phase 5 迁移成本"目标在内存存储维度未闭合

**问题描述**: 需求明确要求"所有包直接落地在底座（而非独立接入），规避 Phase 5 迁移成本"。设计在 §1.1 和 §6.1 中承认三项内存存储（DialogueSessionManager、AiSuggestionResult 存储、PrescriptionDraftContext）在 Phase 5 需全量迁移至分布式缓存，但仅在 §1.1 中以"建议"口吻提及引入 Store 抽象层，实际设计中仍直接使用 `ConcurrentHashMap` 实现，不存在任何隔离层。这意味着 Phase 5 迁移时这三项存储的业务模块代码必须修改，迁移成本未被规避。此前内部审议（迭代第 1 轮 P7）仅修复了"断言矛盾"的表面问题，未解决存储层实际的迁移成本。

**所在位置**: §1.1 "底座直接落地"设计目标（line 9）；§6.1 部署约束（line 991-995）
**严重程度**: 严重
**改进建议**: 将 Store 抽象层（如 `SessionStore`、`SuggestionStore`、`DraftContextStore` 接口）从"建议"升级为"设计强制项"，Phase 2/3 使用 `ConcurrentHashMapStore` 实现，Phase 5 替换为 `RedisStore` 实现。业务 Service 统一依赖接口而非具体实现，确保迁移时代码无须修改。

### 1.2 [一般] "规则可配置"依赖 admin 模块 OOD，无时间线协调

**问题描述**: 需求 3.4.1 要求"规则可配置"。§9.3 将 TriageRule、DrugContraindicationMapping 等实体的 CRUD 管理接口定义为"由 admin 模块 OOD 文档独立定义"，但本设计未定义 admin 模块 OOD 的交付时间线，也未标注若 admin OOD 滞后对本设计交付的影响。这导致"规则可配置"这一需求的实现依赖于外部产出的交付进度，存在不可控的交付风险。

**所在位置**: §9.3 规则管理接口定义（line 1158-1168）
**严重程度**: 一般
**改进建议**: 在 §1.1a 外部依赖表中补充 admin 模块规则管理接口的依赖项，标注实现时间线约束和风险说明（参照现有 registration 和 visit 模块的格式）；或在本设计范围内定义规则管理的简要 API 契约。

---

## 2. 逻辑矛盾与事实错误

### 2.1 [严重] check-dose 端点高频调用下的异步 AI 去重/节流策略完全缺失

**问题描述**: §4.4 定义 check-dose 每次调用生成新 taskId 并以 PENDING 状态预创建 AiSuggestionResult，随后通过 @Async 启动异步 AI 调用。但医生编辑处方时，每次剂量字段变化（如敲击键盘）均可能触发 check-dose 请求。设计未定义：
- 同一 prescriptionId 下，前一次 taskId 对应的异步 AI 调用是否应取消
- 若不加去重，一次编辑会话可能产生数十至数百个并发 AI 调用
- AiSuggestionResult 的 TTL（60 分钟）意味着这些累积的 taskId 在 60 分钟内持续占用内存

§6.3 仅描述 @Async/CompletableFuture.runAsync() 的技术手段，未涉及调用频率控制的业务规则。

**所在位置**: §3.4 PrescriptionAssistService（line 598-601）；§4.4 check-dose 流程（line 830-835）；§6.3 包E 的异步 AI 建议（line 1001-1003）
**严重程度**: 严重
**改进建议**: (a) 定义 prescriptionId 级别的异步 AI 调用去重策略——同一 prescriptionId 下存在 PENDING/COMPLETED 未读 task 时，不重复创建新 AI 调用；(b) 或定义前端防抖/后端限流配合的节流规则；(c) 若需支持覆盖式更新（新的 async 调用取代旧的），需定义取消前次 task 的机制。

### 2.2 [一般] AiResult.success=true 且 data=null 的边界处理未定义

**问题描述**: §2.3 AiResult 泛型要点定义 AiResult 包含 success/data 等字段。§3.1 TriageService 降级判定语义中定义了 AiResult.success=true 且推荐列表为空时的行为，但未定义 AiResult.success=true 且 data=null 的分支。若 AI 实现返回 `AiResult.success(true, null)`（如构造函数调用错误或 data 未被填充），Service 层在解引用 `data.getRecommendedDepartments()` 等调用时会引发 NullPointerException。此场景在 ai-api DTO 为空壳类的过渡阶段尤为容易出现。

**所在位置**: §2.3 AiResult 泛型要点（line 344-345）；§3.1 TriageService 降级判定语义（line 358）；§3.2 PrescriptionAuditService（line 452-454）；§3.3 MedicalRecordService（line 568-572）
**严重程度**: 一般
**改进建议**: (a) 在各 Service 实现类中增加 data=null 的防御性检查，将其等价于降级或空结果处理；(b) 或在 AiResult 类中增加工厂方法约束（如 success() 强制 data 非空）；(c) 在 §2.3 或 §5 中明确此类场景的契约。

### 2.3 [一般] "撤销审核"的操作触发端点未定义

**问题描述**: §3.2 AuditRecord 描述了撤销审核时 isLatest 回退为 false 的逻辑，§4.2 处方提交流程中也提到医生可以执行"撤销审核"操作。但全文未定义触发撤销审核的 API 端点（如 `POST /api/prescription/audit/{auditId}/revoke`），也未描述前端通过何种方式发起撤销请求。缺乏 API 端点的业务行为描述无法直接指导编码。

**所在位置**: §3.2 AuditRecord — 撤销审核时 isLatest 处理（line 482-483）；§4.2 处方提交端点 — WARN 分支三种操作（line 759-761）
**严重程度**: 一般
**改进建议**: 新增撤销审核的 API 端点定义（请求/响应/状态码/逻辑时序），或将撤销行为纳入已有端点（如 `POST /api/prescription/audit` 的扩展参数）。

### 2.4 [轻微] §4.5 命名空间区分说明处有多余字符

**问题描述**: §4.5 (line 888) "ai-api 层与业务层 DTO 命名空间区分：："中出现了双冒号"：："，影响文档一致性。

**所在位置**: §4.5 line 888
**严重程度**: 轻微
**改进建议**: 删除多余的冒号。

---

## 3. 深度与完整性

### 3.1 [一般] prescriptionId 在 check-dose 请求链路中的传递路径未闭环

**问题描述**: §4.4 check-dose 请求包含 prescriptionId，DosageThresholdService 据此将 CRITICAL 告警写入 PrescriptionDraftContext。但 §1.3 DosageCheckRequest 定义中未明确 prescriptionId 的生成时机和来源——prescriptionId 是"草稿创建时分配"（§3.2 AuditRecord 定义），但 check-dose 端点可能先于"创建草稿"动作被调用（用户在新建处方界面的第一个剂量输入即可触发 check-dose）。若 prescriptionId 尚不存在，check-dose 请求中应传入什么值？

**所在位置**: §1.3 DosageCheckRequest 条目（line 127-128）；§3.4 PrescriptionDraftContext（line 627-629）；§4.4 check-dose 流程（line 826-841）
**严重程度**: 一般
**改进建议**: (a) 明确定义 prescriptionId 在处方编辑生命周期中的分配时机——是由前端在进入处方编辑界面时预创建还是后端在首次 check-dose 调用时自动生成；(b) 前端首次调用 check-dose 前若 prescriptionId 尚不存在，定义创建策略。

### 3.2 [一般] 缺少 AI Mock 实现的行为契约

**问题描述**: §1.2 目录结构中标注 `modules/ai/ai-impl` 包含"Mock/降级/底座管线"，但全文未定义 AI Mock 实现的行为契约。开发者在本地开发时需要知道：
- 如何激活 Mock 模式（profile/配置开关）
- Mock 返回哪些数据（固定的推荐科室？空列表？超时模拟？）
- Mock 与真实 AI 实现间的切换机制

作为"直接指导编码实现"的 OOD 文档，此缺失会影响开发效率。

**所在位置**: §1.2 整体架构思路（line 41）；§2.1 目录结构 ai/ai-api 子树（line 239-247）
**严重程度**: 一般
**改进建议**: 在 §2.3 AiService 接口定义后新增"Mock 实现契约"段落，定义 MockAiService 的返回策略和激活方式，或引用 ai-impl 模块的独立设计文档。

### 3.3 [轻微] DosageStandard 单位字段 unit 缺少枚举约束定义

**问题描述**: §8.4 定义 DosageStandard.singleMax 和 unit 字段，但 unit 的类型仅标注为 `String`，未约束为 DosageUnitGroup 中的单位值。这可能导致管理员在 admin 模块写入 DosageStandard 时输入非标准单位（如 "mg/ml"、"片"等），导致 DosageThresholdService 的单位一致性校验因无法匹配 DosageUnitGroup 而全部进入跨组不可比较分支（输出错误 `RX_ASSIST_UNIT_MISMATCH`）。

**所在位置**: §8.4 DosageStandard 实体分级字段表（line 1121）
**严重程度**: 轻微
**改进建议**: 将 unit 字段类型从 `String` 改为对 DosageUnitGroup 的单位枚举引用（如 `String` 校验格式或关联字典表），或在 admin 模块 DosageStandard 保存接口中做单位合法性校验。

---

## 4. 综合评价

本设计文档在经历了多轮内部审议（v8→v21 逐轮修订）后，整体质量较高。各包的职责划分、核心抽象定义、关键行为契约和错误处理策略均已有较完备的覆盖。以下三个维度各有遗留问题：

| 审查维度 | 结果 |
|---------|------|
| 需求响应充分度 | 基本覆盖全部需求点，但"规避 Phase 5 迁移成本"目标在内存存储维度未闭合，且"规则可配置"存在外部依赖协调缺失 |
| 事实/逻辑正确性 | 未发现事实错误；但 check-dose 异步调度缺少去重策略、撤销审核缺少触发 API，属逻辑契约不完整 |
| 深度与完整性 | 可满足大部分编码需求，但在高频调用节流、Mock 实现契约、prescriptionId 生命周期边界等场景缺少直接可执行的规范 |
