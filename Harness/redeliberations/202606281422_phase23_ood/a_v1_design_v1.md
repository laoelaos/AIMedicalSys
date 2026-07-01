# Phase 2/3 包C/D-AI1/D-AI2/E — 架构级 OOD 设计方案

## 1. 概述

### 1.1 设计目标

本设计覆盖 Phase 2 包C（智能分诊）与 Phase 3 包D-AI1（处方审核）、包D-AI2（病历生成）、包E（辅助开方）四个业务包。核心目标如下：

- **底座直接落地**：四个包均直接以 Maven 模块形式落地在 AIMedical 后端底座上，遵循 existing common → modules → application 的分层架构，规避后续迁移至 Phase 5 AI 进阶底座的重构成本
- **架构风格一致**：四个包的模块结构、依赖方向和抽象层次与 Phase 0（骨架模块）、Phase 1（认证模块）的风格保持一致
- **强耦合同步落地**：包E（辅助开方）与包D-AI1（处方审核）共享处方领域数据和业务规则，设计为同一模块内的两个子域，同步开发、同步发布
- **AI 能力集成标准化**：四个包的业务逻辑均通过 `ai-api` 中的 `AiService` 接口调用 AI 能力，隔离 AI 实现细节；AI 不可用时的降级路径由各自模块的本地规则兜底，与 AiService 的降级框架协同

### 1.2 整体架构思路

```
application（启动聚合层）
  │
  ├── modules/consultation/       包C 智能分诊
  │     └── 依赖: common, ai-api
  │
  ├── modules/prescription/       包D-AI1 处方审核 + 包E 辅助开方
  │     └── 依赖: common, ai-api
  │
  ├── modules/medical-record/     包D-AI2 病历生成
  │     └── 依赖: common, ai-api
  │
  ├── modules/ai/ai-impl          AI 实现（含 Mock/降级/底座管线）
  ├── modules/common-module/      公共业务模块（认证/权限/用户）
  ├── modules/patient/            患者模块
  ├── modules/doctor/             医生模块
  ├── modules/admin/              管理员模块
  └── common/                     共享基类与契约
```

三个新模块均为**扁平 Maven 模块**（不拆分为 api/impl 子模块），与 patient/doctor/admin 的结构一致。每个模块内部按职责分包：`api`（REST Controller）、`service`（业务接口与实现）、`repository`（JPA Repository）、`entity`（JPA 实体）、`dto`（请求/响应 DTO）、`converter`（实体/DTO 转换）。各模块仅依赖 `common` 和 `ai-api`，不依赖其他业务模块的 impl 层。

### 1.3 核心抽象一览

#### 包C（智能分诊）

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| `TriageController` | class | 分诊对话 REST 端点，接收前端提交的主诉/追问答复，返回推荐科室列表 |
| `TriageService` | interface | 智能分诊业务契约，封装单轮/多轮对话的分诊逻辑、对话状态管理和规则回退 |
| `DialogueSession` | class | 多轮对话会话值对象，维护当前 session 的对话上下文、已收集的症状信息和当前轮次 |
| `DialogueSessionManager` | class | 对话会话生命周期管理器，负责 session 的创建、查找、更新和过期清理 |
| `TriageRuleEngine` | interface | 分诊规则引擎契约，根据症状匹配规则返回推荐科室；支持可配置规则源 |
| `DepartmentFallbackProvider` | interface | 兜底科室列表提供者契约，当 AI 不可用时返回静态部门列表或基于简单规则的匹配结果 |

#### 包D-AI1（处方审核）

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| `PrescriptionAuditController` | class | 处方审核 REST 端点，接收待审核处方，返回审核结果与风险等级 |
| `PrescriptionAuditService` | interface | 处方审核业务契约，协调 AI 审核调用与本地规则回退 |
| `AuditRiskLevel` | enum | 风险等级枚举，定义 PASS（通过）、WARN（警告）、BLOCK（阻断）三个级别，决定前端行为 |
| `AuditRecord` | JPA @Entity | 审核记录持久化实体，保存每次审核的原始处方、AI 结果、风险等级和时间戳 |
| `LocalRuleEngine` | interface | 本地规则校验引擎契约，封装药物相互作用、过敏检查、剂量上限等规则；AI 超时或不可用时作为降级回退 |

#### 包D-AI2（病历生成）

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| `MedicalRecordController` | class | 病历生成 REST 端点，接收医生端对话文本，返回结构化病历 |
| `MedicalRecordService` | interface | 病历生成业务契约，协调 AI 结构化输出与科室模板配置 |
| `DepartmentTemplateConfig` | class | 科室模板配置值对象，按科室标识管理病历生成规则和模板版本 |
| `TemplateConfigManager` | interface | 科室模板配置管理器契约，支持模板的运行时查询与缓存 |
| `MissingFieldDetector` | interface | 关键字段缺失检测器契约，识别 AI 输出中缺失的必填字段并生成补全提示 |

#### 包E（辅助开方）

| 抽象 | 类型形态 | 职责定位 |
|------|---------|---------|
| `PrescriptionAssistController` | class | 辅助开方 REST 端点，接收医生开方过程中的药品和剂量信息，返回剂量告警和合理性建议 |
| `PrescriptionAssistService` | interface | 辅助开方业务契约，剂量阈值检查与处方合理性分析 |
| `DosageThresholdService` | class | 剂量阈值校验服务，按药品、年龄、体重维度检查剂量是否超限 |
| `DosageAlert` | class | 剂量告警值对象，封装告警级别、告警消息和建议调整值 |

---

## 2. 模块划分

### 2.1 目录结构

```
backend/modules/
├── consultation/                            # 包C 智能分诊
│   ├── pom.xml
│   └── src/main/java/com/aimedical/modules/consultation/
│       ├── api/
│       │   └── TriageController.java
│       ├── dto/
│       │   ├── TriageRequest.java           # 分诊请求（含主诉、sessionId、对话历史上下文）
│       │   ├── TriageResponse.java          # 分诊响应（推荐科室列表、推理理由、是否需要继续追问）
│       │   └── DialogueCreateRequest.java   # 创建新会话请求
│       ├── service/
│       │   ├── TriageService.java           # 分诊业务接口
│       │   └── impl/
│       │       └── TriageServiceImpl.java   # 分诊业务实现
│       ├── dialogue/
│       │   ├── DialogueSession.java         # 多轮会话值对象
│       │   └── DialogueSessionManager.java  # 会话生命周期管理
│       ├── rule/
│       │   ├── TriageRuleEngine.java        # 分诊规则引擎接口
│       │   └── DefaultTriageRuleEngine.java # 默认规则引擎实现（基于配置的科室映射表）
│       ├── fallback/
│       │   ├── DepartmentFallbackProvider.java # 兜底科室列表提供者接口
│       │   └── StaticDepartmentFallbackProvider.java # 静态兜底实现
│       ├── repository/
│       │   └── TriageRecordRepository.java  # 分诊记录 JPA Repository
│       ├── entity/
│       │   └── TriageRecord.java            # 分诊记录 JPA 实体
│       └── converter/
│           └── TriageConverter.java         # 分诊实体/DTO 转换
│
├── prescription/                            # 包D-AI1 处方审核 + 包E 辅助开方
│   ├── pom.xml
│   └── src/main/java/com/aimedical/modules/prescription/
│       ├── api/
│       │   ├── audit/
│       │   │   └── PrescriptionAuditController.java
│       │   └── assist/
│       │       └── PrescriptionAssistController.java
│       ├── dto/
│       │   ├── audit/
│       │   │   ├── AuditRequest.java        # 审核请求（含处方明细、患者信息）
│       │   │   ├── AuditResponse.java       # 审核响应（风险等级、审核意见、问题项列表）
│       │   │   └── AuditIssue.java          # 审核问题项值对象（问题描述、问题等级、涉及药品）
│       │   └── assist/
│       │       ├── DosageCheckRequest.java  # 剂量检查请求（药品、剂量、患者体征）
│       │       ├── DosageAlert.java         # 剂量告警值对象
│       │       └── PrescriptionAssistResponse.java # 辅助开方响应（告警列表、合理性建议）
│       ├── service/
│       │   ├── audit/
│       │   │   ├── PrescriptionAuditService.java       # 审核业务接口
│       │   │   ├── impl/
│       │   │   │   └── PrescriptionAuditServiceImpl.java
│       │   │   └── AuditRiskLevel.java                 # 风险等级枚举
│       │   └── assist/
│       │       ├── PrescriptionAssistService.java       # 辅助开方业务接口
│       │       ├── impl/
│       │       │   └── PrescriptionAssistServiceImpl.java
│       │       └── DosageThresholdService.java          # 剂量阈值校验服务
│       ├── rule/
│       │   ├── LocalRuleEngine.java        # 本地规则引擎接口
│       │   ├── DrugInteractionRule.java    # 药物相互作用规则
│       │   ├── AllergyCheckRule.java       # 过敏检查规则
│       │   ├── DosageLimitRule.java        # 剂量上限规则
│       │   └── LocalRuleResult.java        # 规则检查结果值对象
│       ├── repository/
│       │   ├── AuditRecordRepository.java  # 审核记录 Repository
│       │   └── DosageStandardRepository.java # 剂量标准 Repository
│       ├── entity/
│       │   ├── AuditRecord.java            # 审核记录 JPA 实体
│       │   └── DosageStandard.java          # 剂量标准 JPA 实体（药品剂量上限参考）
│       └── converter/
│           ├── AuditConverter.java         # 审核实体/DTO 转换
│           └── AssistConverter.java        # 辅助开方实体/DTO 转换
│
├── medical-record/                          # 包D-AI2 病历生成
│   ├── pom.xml
│   └── src/main/java/com/aimedical/modules/medicalrecord/
│       ├── api/
│       │   └── MedicalRecordController.java
│       ├── dto/
│       │   ├── RecordGenerateRequest.java  # 病历生成请求（含对话文本、科室标识、患者ID）
│       │   ├── RecordGenerateResponse.java # 病历生成响应（结构化病历、字段补全提示列表）
│       │   └── FieldMissingHint.java       # 字段缺失提示值对象（缺失字段名、建议补充内容）
│       ├── service/
│       │   ├── MedicalRecordService.java   # 病历生成业务接口
│       │   └── impl/
│       │       └── MedicalRecordServiceImpl.java
│       ├── template/
│       │   ├── TemplateConfigManager.java  # 科室模板配置管理器接口
│       │   ├── DepartmentTemplateConfig.java # 科室模板配置值对象
│       │   └── DatabaseTemplateConfigManager.java # 基于数据库的模板配置实现
│       ├── parser/
│       │   └── MissingFieldDetector.java   # 关键字段缺失检测器接口
│       ├── repository/
│       │   ├── MedicalRecordRepository.java
│       │   └── DeptTemplateConfigRepository.java
│       ├── entity/
│       │   ├── MedicalRecord.java          # 病历 JPA 实体
│       │   └── DeptTemplateConfig.java     # 科室模板配置 JPA 实体
│       └── converter/
│           └── MedicalRecordConverter.java
```

### 2.2 模块职责与依赖方向

#### 各模块依赖关系

```
modules/consultation ───> common
                    ───> ai-api

modules/prescription ───> common
                    ───> ai-api

modules/medical-record ───> common
                       ───> ai-api

application ───> modules/consultation
            ───> modules/prescription
            ───> modules/medical-record
            ───> modules/patient, doctor, admin, ai-impl, common-module-impl
```

**依赖规则：**

- 三个新模块均以 `compile` scope 依赖 `common` 和 `ai-api`；`ai-api` 中的依赖在 Maven `dependency:analyze` 中通过 `<ignoredUnusedDeclaredDependencies>` 豁免（与现有 patient/doctor/admin 模块的处理一致），直到业务代码产生真实引用
- 三个新模块之间**不允许互相依赖**。若后续出现跨模块协作需求（如处方审核引用患者过敏信息），通过以下两种方式之一实现：
  - **门面接口**：在涉及的模块中通过 REST 调用或公共门面接口解耦
  - **事件驱动**：通过 `ApplicationEventPublisher` 广播事件，避免编译期耦合
- `application` 模块在 POM 中引入三个新模块的依赖（`compile` scope），Spring Boot 包扫描配置 `scanBasePackages = "com.aimedical"` 已覆盖新模块路径，无需额外配置
- `spring-boot-starter-web`、`spring-boot-starter-data-jpa`、`spring-boot-starter-validation` 按现有模式由各模块在自身 POM 中显式引入

#### 包D-AI1 与包E 的强耦合处理

包E（辅助开方）与包D-AI1（处方审核）共享同一个 `prescription` 模块，遵循以下协作约定：

- **共享数据**：`DosageStandard` 实体（药品剂量标准）由审核和辅助开方两个子域共同写入/读取；`AuditRecord` 实体（审核记录）由审核子域写入，辅助开方子域读取以感知最新审核结果
- **服务调用**：`PrescriptionAssistServiceImpl` 内部可注入 `PrescriptionAuditService` 接口直接调用审核逻辑，无需通过 REST 或事件——两者属同一 Maven 模块，编译期不产生循环依赖
- **前端交互**：医生开方流程中，辅助开方建议展示在处方编辑界面底部，审核结果展示在提交时的弹窗；两者通过同一个 `PrescriptionAssistController` 端点或两个独立端点按需调用

#### 与 AI 模块的协作关系

三个模块各自通过注入 `AiService` 接口调用 AI 能力（智能分诊调用 `triage()`、处方审核调用 `prescriptionCheck()`、病历生成调用 `generateMedicalRecord()`、辅助开方调用 `prescriptionAssist()`）。`AiService` 的降级框架（`FallbackAiService` + 降级策略）对业务模块完全透明。

各模块在 AI 调用结果 `AiResult.degraded = true` 时需提供本地规则兜底行为，而非直接返回"AI 不可用"给前端。兜底行为分别由：
- `DepartmentFallbackProvider`（包C）
- `LocalRuleEngine`（包D-AI1）
- 固定格式病历模板 + 完整字段缺失提示列表（包D-AI2）
- `DosageThresholdService` 直接校验（包E，剂量阈值检查无需 AI）

---

## 3. 核心抽象

### 3.1 包C：智能分诊

#### `TriageService` — 智能分诊业务接口（interface）

**职责**：定义智能分诊的核心业务边界，覆盖单轮分诊、多轮追问和会话管理三种场景。

**协作对象**：
- 被 `TriageController` 调用，接收前端分诊请求和追问答复
- 内部委托 `AiService.triage()` 获取 AI 分诊结果
- 内部委托 `TriageRuleEngine` 在 AI 结果为空或不可用时执行规则匹配
- 内部委托 `DepartmentFallbackProvider` 在 AI 和规则引擎均无法决策时返回静态兜底科室列表
- 管理 `DialogueSessionManager` 实现多轮对话的上下文维护

**为何使用 interface**：分诊业务可能存在多种实现（普通分诊 vs 急诊分诊），interface 允许在后续阶段增加实现变体而不影响调用方。

#### `DialogueSession` — 多轮对话会话值对象（class）

**职责**：封装一次多轮分诊对话的完整上下文。核心状态包括：会话唯一标识、患者主诉、已收集的症状信息列表、当前追问轮次、AI 中间推理结果、当前会话状态（进行中/已完成/已超时）。

**协作**：被 `DialogueSessionManager` 创建和持久化，被 `TriageServiceImpl` 在每轮迭代中读取和追加。

**为何使用不可变 class**：贯穿多个请求的会话状态需要明确的读写边界，不可变值对象避免引用逃逸导致的并发问题。

#### `DialogueSessionManager` — 对话会话生命周期管理器（class）

**职责**：管理 `DialogueSession` 的创建、按 sessionId 查找、状态更新和过期清理。

**协作**：
- 被 `TriageService` 在每次分诊请求时调用，用于创建新会话或恢复已有会话
- 内部维护 `ConcurrentHashMap<String, DialogueSession>` 作为运行时存储，配合 `ScheduledExecutorService` 定期清理过期会话

**为何使用 class 而非 interface**：会话存储策略（内存 → Phase 5 Redis）虽有变更可能，但管理器的职责边界稳定——创建、查询、更新、删除——interface 的抽象收益在当前阶段不抵实现复杂度。

#### `TriageRuleEngine` — 分诊规则引擎接口（interface）

**职责**：根据症状-科室匹配规则，将主诉或已收集的症状列表映射到推荐科室集。规则可配置（数据库或 YAML），支持按科室优先级排序。

**协作**：
- 被 `TriageService` 在 AI 分诊结果为空或置信度不足时调用
- 内部持有规则配置源（数据库表或配置文件），为规则渲染提供数据

**为何使用 interface**：规则源可能从配置文件演化为数据库管理，interface 隔离规则消费方与规则存储实现。

#### `DepartmentFallbackProvider` — 兜底科室列表提供者接口（interface）

**职责**：当 AI 分诊和规则引擎均无法给出推荐科室时，返回预配置的静态科室列表作为兜底。

**协作**：被 `TriageService` 在降级路径末尾调用，返回 `List<DepartmentInfo>`。

**为何使用 interface**：兜底策略允许扩展——从当前静态列表到基于简单关键词匹配的智能兜底，interface 提供扩展点而不影响调用方。

### 3.2 包D-AI1：处方审核

#### `PrescriptionAuditService` — 处方审核业务接口（interface）

**职责**：定义处方审核的核心流程：接收待审核处方 → 调用 AI 审核 → AI 超时/不可用时回退本地规则 → 返回风险等级和审核意见。

**协作对象**：
- 被 `PrescriptionAuditController` 调用
- 内部委托 `AiService.prescriptionCheck()` 获取 AI 审核结果
- 内部委托 `LocalRuleEngine` 在 AI 超时或不可用时执行本地规则检验
- 内部写入 `AuditRecord` 实体持久化审核记录

**为何使用 interface**：审核流程可能会扩展（如加入人工审核队列、外部药监接口），interface 保持契约稳定。

#### `AuditRiskLevel` — 风险等级枚举（enum）

**职责**：定义处方审核结果的三个风险级别及其对应的前端行为。
- `PASS`：审核通过，无风险项，前端不阻断
- `WARN`：存在低风险问题（如轻微相互作用），前端提示但允许提交
- `BLOCK`：存在高风险问题（如致死性相互作用或剂量超标），前端阻断提交

**为何使用 enum**：风险等级是固定、有限的分类集合，没有扩展层次的需求。enum 提供编译期类型安全和 switch 完备性检查。

#### `AuditRecord` — 审核记录持久化实体（JPA @Entity）

**职责**：持久化每次处方审核的全部元数据，用于审计追溯和风险分析。

**协作**：被 `PrescriptionAuditService` 在每次审核完成后写入。

**为何使用 JPA @Entity**：审核记录需要持久化存储和查询，JPA 实体自然映射到数据库表，与项目现有的数据持久化方案一致。

#### `LocalRuleEngine` — 本地规则校验引擎接口（interface）

**职责**：封装一组本地校验规则，包括：药物相互作用检查（DrugInteractionRule）、过敏史检查（AllergyCheckRule）、剂量上限检查（DosageLimitRule）。每条规则接受处方上下文，返回 `LocalRuleResult`（含是否通过、问题描述、风险等级）。

**协作对象**：
- 被 `PrescriptionAuditService` 在 AI 不可用或超时时调用
- 各规则实现通过构造器注入所需的数据访问对象（如药品信息 Repository）

**为何使用 interface + 多条独立规则**：每条规则独立实现、独立测试、独立启用/禁用，新增规则只需新增实现类即可注册到规则引擎中。规则链的设计避免了一个庞大的规则方法。

### 3.3 包D-AI2：病历生成

#### `MedicalRecordService` — 病历生成业务接口（interface）

**职责**：定义对话文本到结构化病历的转换流程：接收对话文本和科室标识 → 按科室模板生成 Prompt → 调用 AI 结构化输出 → 检测缺失字段 → 返回结构化病历和补全提示。

**协作对象**：
- 被 `MedicalRecordController` 调用
- 内部委托 `AiService.generateMedicalRecord()` 获取 AI 结构化病历
- 内部委托 `TemplateConfigManager` 获取当前科室的生成规则
- 内部委托 `MissingFieldDetector` 检查输出完整性

**为何使用 interface**：病历生成规则可能按科室演变，interface 为不同科室的生成策略变体预留扩展点。

#### `DepartmentTemplateConfig` — 科室模板配置值对象（class）

**职责**：封装单个科室的病历生成规则配置，包括：科室标识、模板版本号、字段映射规则、必填字段列表、Prompt 模板内容。

**协作**：被 `TemplateConfigManager` 按科室标识查询，被 `MedicalRecordService` 在生成 Prompt 时读取。

**为何使用 class 而非 interface**：配置数据是固定的键值对集合，无多态行为，class 是最直接的承载方式。

#### `TemplateConfigManager` — 科室模板配置管理器接口（interface）

**职责**：按科室标识和版本号查询模板配置，支持运行期热加载（缓存失效后重新查询数据库）。

**协作**：
- 被 `MedicalRecordService` 在每次生成请求开始时调用
- 内部维护 `LoadingCache`（Caffeine 或 Guava）缓存模板配置，配置更新后通过缓存失效机制刷新

**为何使用 interface**：缓存策略可替换（本地缓存 → 分布式缓存），interface 隔离缓存实现与业务消费方。

#### `MissingFieldDetector` — 关键字段缺失检测器接口（interface）

**职责**：检测 AI 输出的结构化病历中是否缺少当前科室配置的必填字段，如果缺失则生成补全提示。

**协作**：
- 被 `MedicalRecordService` 在 AI 返回结构化病历后调用
- 内部读取 `DepartmentTemplateConfig` 中的必填字段列表作为检测基准

**为何使用 interface**：字段缺失检测策略可扩展——从简单的 null/空串检查到语义级缺失检测，interface 隔离检测逻辑与消费方。

### 3.4 包E：辅助开方

#### `PrescriptionAssistService` — 辅助开方业务接口（interface）

**职责**：定义处方合理性辅助检查的业务边界：接收医生正在录入的药品和剂量 → 执行剂量阈值检查 → 返回告警列表。

**协作对象**：
- 被 `PrescriptionAssistController` 调用（前端在医生输入药品和剂量时实时触发）
- 内部委托 `DosageThresholdService` 执行剂量校验
- 内部委托 `PrescriptionAuditService` 获取最新审核结果作为辅助参考（可选）
- 内部委托 `AiService.prescriptionAssist()` 获取 AI 合理性建议（在后端异步调用，不影响剂量阈值检查的即时响应）

**为何使用 interface**：辅助开方未来可能扩展更多检查维度（药物-食物相互作用、肾功能调整剂量等），interface 保持契约稳定。

#### `DosageThresholdService` — 剂量阈值校验服务（class）

**职责**：按药品编码、患者年龄、体重等维度查询剂量标准（`DosageStandard`），比较录入剂量是否在安全范围内。超限时生成 `DosageAlert`。

**协作对象**：
- 被 `PrescriptionAssistService` 调用
- 内部委托 `DosageStandardRepository` 查询标准剂量配置

**为何使用 class**：剂量阈值校验逻辑稳定，无多态变体需求；直接使用 class 降低不必要的抽象层次。

#### `DosageAlert` — 剂量告警值对象（class）

**职责**：封装一条剂量告警的完整信息：告警级别（WARN/BLOCK）、告警消息（如"xx 药品单次剂量超出成人最大推荐剂量"）、药品编码、当前剂量、建议调整值。

**为何使用 class**：纯粹的数据容器，无业务行为，class 是最直接的选择。

---

## 4. 关键行为契约

### 4.1 智能分诊场景

#### 单轮分诊流程

```
POST /api/triage/consult
  请求体: { chiefComplaint: "胸痛伴气短" }
  → TriageController
    → TriageService.triage(TriageRequest)

正常路径:
  → AiService.triage(TriageRequest)
    → CompletableFuture<AiResult<TriageResponse>>
    → AI 成功 → 解析推荐科室列表 → 返回 List<RecommendedDepartment>

AI 空结果或置信度不足:
  → TriageRuleEngine.match(chiefComplaint)
    → 按可配置规则匹配科室
    → 匹配成功 → 返回规则匹配的科室列表

AI 不可用 / 规则引擎无匹配:
  → DepartmentFallbackProvider.getFallbackDepartments()
    → 返回预配置的静态兜底科室列表（如"内科"、"全科"）

响应: Result<TriageResponse> {
  recommendedDepartments: [...],
  triageReason: "...",
  requiresFollowUp: false,
  fromFallback: true/false
}
```

#### 多轮分诊流程

```
POST /api/triage/consult
  请求体: { sessionId: "xxx", chiefComplaint: "胸痛", history: [...] }
  → TriageController
    → DialogueSessionManager.findOrCreate(sessionId, chiefComplaint)
      → session 不存在 → 创建新会话（首次请求）
      → session 存在且未完成 → 恢复已有会话（后续追问）

正常路径（第一轮）:
  → AiService.triage(request with chiefComplaint)
  → AI 返回推荐科室 + 可能需要追问的问题
  → AI 确认需要追问 → TriageResponse.requiresFollowUp = true
    → followUpQuestion: "疼痛是持续性还是阵发性？"
    → 前端展示追问并收集用户回复
    → DialogueSession 追加本轮 QA

正常路径（第二轮及以后）:
  → AiService.triage(request with chiefComplaint + 对话历史)
  → AI 结合全部上下文给出最终推荐科室
  → TriageResponse.requiresFollowUp = false
  → DialogueSession 标记为已完成

超时/降级路径（同单轮分诊的规则/兜底回退）
```

### 4.2 处方审核场景

```
POST /api/prescription/audit
  请求体: { prescriptionItems: [...], patientInfo: {...} }
  → PrescriptionAuditController
    → PrescriptionAuditService.audit(AuditRequest)

正常路径:
  → AiService.prescriptionCheck(AuditRequest)
    → CompletableFuture<AiResult<PrescriptionCheckResponse>>
    → AI 成功 → 解析风险等级和审核意见
    → 结果持久化到 AuditRecord
    → 返回 AuditResponse { riskLevel: PASS/WARN/BLOCK, issues: [...] }

AI 超时/不可用（降级路径）:
  → LocalRuleEngine.check(prescription)
    → 依次执行 DrugInteractionRule → AllergyCheckRule → DosageLimitRule
    → 聚合各规则结果 → 合并为最终的 riskLevel 和 issues
    → 结果持久化到 AuditRecord（标注 fromFallback=true）
    → 返回 AuditResponse

AI 业务异常（非降级）:
  → 直接返回 AiResult.failure() 中包含的错误信息
  → 不执行本地规则回退（AI 调用成功但返回业务错误）
```

### 4.3 病历生成场景

```
POST /api/medical-record/generate
  请求体: { dialogueText: "...", departmentId: "NEUROLOGY", patientId: 123 }
  → MedicalRecordController
    → MedicalRecordService.generate(RecordGenerateRequest)

正常路径:
  → TemplateConfigManager.getTemplate("NEUROLOGY")
    → 加载神经内科的病历模板配置（必填字段、结构化规则）
  → AiService.generateMedicalRecord(request with department-specific prompt)
    → 返回 CompletableFuture<AiResult<MedicalRecordGenResponse>>
    → AI 成功 → 解析结构化病历
  → MissingFieldDetector.detect(structuredRecord, templateConfig)
    → 检查必填字段（主诉、现病史、既往史等）是否缺失
    → 生成 List<FieldMissingHint>
  → 返回 RecordGenerateResponse {
      structuredRecord: { ... },
      missingFields: [ { fieldName: "既往史", suggestedContent: "请补充..." } ]
    }

AI 不可用（降级路径）:
  → TemplateConfigManager.getTemplate(departmentId)
    → 加载该科室的空模板结构
  → 返回固定格式的空病历框架
  → MissingFieldDetector 标记所有必填字段为缺失
  → 返回 RecordGenerateResponse（所有字段标记缺失，标注 fromFallback=true）
```

### 4.4 辅助开方场景

```
POST /api/prescription/assist/check-dose
  请求体: { drugCode: "xxx", dosage: 500, unit: "mg", patientAge: 65, patientWeight: 70 }
  → PrescriptionAssistController
    → PrescriptionAssistService.checkDosage(DosageCheckRequest)

正常路径:
  → DosageThresholdService.check(drugCode, dosage, patientInfo)
    → DosageStandardRepository.findByDrugCode(drugCode)
    → 比较剂量与标准阈值（单次上限、日上限、按体重调整值）
    → dosage 在安全范围内 → 返回空告警列表
    → dosage 超限 → 生成 DosageAlert(WARN/BLOCK, message, suggestedDosage)
  → 异步调用 AiService.prescriptionAssist() 获取 AI 合理性建议（不阻塞响应）

降级路径:
  → 剂量阈值检查不依赖 AI，直接基于本地标准数据
  → 无降级路径（本地检查始终可用）

响应: Result<PrescriptionAssistResponse> {
  alerts: [ { level: "WARN", message: "...", suggestedAdjustment: 250 } ],
  aiSuggestion: null/可选
}
```

---

## 5. 错误处理策略

### 5.1 模块级错误码

| 错误类别 | 错误码前缀 | 代表场景 | 处理方式 |
|---------|-----------|---------|---------|
| 分诊错误 | `TRIAGE_` | 会话超时、分诊参数缺失 | `BusinessException` → `Result` |
| 审核错误 | `RX_AUDIT_` | 处方格式不合法、药品编码不存在 | `BusinessException` → `Result` |
| 病历错误 | `MR_` | 对话文本过短、科室模板不存在 | `BusinessException` → `Result` |
| 开方辅助错误 | `RX_ASSIST_` | 药品剂量标准不存在、剂量数据异常 | `BusinessException` → `Result` |

### 5.2 AI 降级作为正常业务流程

- AI 超时或不可用时，各模块的本地规则回退视为**正常业务流程**而非异常
- 响应体中通过 `fromFallback: true` 标记告知前端当前结果是降级输出
- 前端根据 `fromFallback` 标记可选地展示"AI 暂不可用，已使用本地规则"提示，但不阻断用户操作
- 包C 的 AI 降级回退至规则引擎 → 静态兜底科室列表
- 包D-AI1 的 AI 降级回退至本地规则引擎
- 包D-AI2 的 AI 降级回退至空病历模板 + 全字段缺失提示
- 包E 的剂量阈值检查不依赖 AI，无降级路径

### 5.3 与已有异常处理框架的集成

三个模块的异常处理复用 Phase 0 定义的 `GlobalExceptionHandler` + `ErrorCode` 接口 + `BusinessException` 体系。各模块自定义的 `ErrorCode` enum 实现 `ErrorCode` 接口，按模块前缀在 `ExceptionHandler` 中统一处理。

---

## 6. 并发设计

### 6.1 对话会话并发管理

包C 的多轮会话状态（`DialogueSession`）使用 `ConcurrentHashMap` 存储，以 `sessionId` 为键。每次分诊请求时：
- 同 session 的多轮请求是串行的（前端在收到上一轮响应后才发起下一轮请求），不存在同 session 的并发写竞争
- 不同 session 的请求完全独立，`ConcurrentHashMap` 保证无锁安全
- 会话过期清理使用 `ScheduledExecutorService` 定时任务，清理逻辑对运行时请求无影响

### 6.2 AI 调用并发

三个模块的 AI 调用统一复用 `CompletableFuture<AiResult<T>>` 契约（定义于 Phase 0）。各模块在 Service 层同步等待 AI 结果（`join()` 或 `get(timeout)`），不引入额外的线程池管理。若后续需要编排多个 AI 调用，可在 Service 层组合 `CompletableFuture`。

### 6.3 包E 的异步 AI 建议

`PrescriptionAssistService.checkDosage()` 在返回剂量阈值检查结果后，可使用 `@Async` 或 `CompletableFuture.runAsync()` 异步调用 `AiService.prescriptionAssist()`。主响应不等待 AI 结果，AI 建议以可选字段形式在后续查询接口中提供。此异步行为不阻塞主请求响应。

---

## 7. 设计决策

| 决策 | 选项 | 选择 | 理由 |
|------|------|------|------|
| 模块粒度 | 4 个独立模块 vs 2 个模块（C 独立、D-AI1+E 合并、D-AI2 独立） | 3 个模块 | 包C（智能分诊）和包D-AI2（病历生成）独立性强，各自对应独立的前端交互场景；包D-AI1 与包E 共享处方领域数据（药品信息、剂量标准），且需同步落地，合并为同一模块避免跨模块数据共享的复杂度 |
| 模块结构 | 扁平模块（如 patient） vs api/impl 子模块（如 ai） | 扁平模块 | 三个新模块主要对外暴露 REST API，不提供跨模块门面接口——没有多个实现需要隐藏的场景。扁平模块结构与现有 patient/doctor/admin 一致，降低理解和维护成本 |
| 多轮对话存储 | 内存 vs 数据库 | 内存（`ConcurrentHashMap` + 定时清理） | Phase 2/3 范围中，分诊对话是短时间内完成的交互（通常 2-5 轮，持续几分钟），不涉及跨会话持久化需求。内存方案性能好、实现简单。Phase 5 需要历史分析和跨设备会话同步时间迁移至数据库 |
| 分诊规则源 | YAML 配置 vs 数据库 | 数据库（配置表） + 启动时缓存 | 分诊规则需要支持非开发人员（业务运营）动态调整，YAML 配置无法满足频繁变更。数据库存储配合启动时缓存或 Caffeine 缓存满足低延迟查询 |
| 处方审核本地规则实现形态 | 单一大引擎类 vs 多条独立规则实现 | 多条独立规则实现（`LocalRuleEngine` 链） | 每条规则独立实现、独立测试、独立启用。新增规则只需新增 `interface` 实现类，不修改现有规则代码。符合开闭原则 |
| 病历生成科室模板配置 | 模板代码中硬编码 vs 数据库可配置 | 数据库 `DeptTemplateConfig` 实体 + `TemplateConfigManager` 接口 | 支持不同科室的差异化字段映射和必填项配置，非开发人员可通过管理界面调整模板。`TemplateConfigManager` 统一管理查询和缓存逻辑，屏蔽存储细节 |
| 包E 的 AI 调用时机 | 同步（阻塞主响应） vs 异步（不阻塞） | 剂量阈值检查同步返回 + AI 建议异步调用 | 剂量阈值是即时性要求高的安全检查，必须同步返回结果阻塞前端操作。AI 合理性建议是增值信息，异步调用不增加主响应延迟 |
| 包E 与包D-AI1 的协作方式 | 事件驱动 vs 直接方法调用 | 同一模块内的直接方法调用 | 两者同属一个 Maven 模块，直接调用不产生循环依赖。如果未来分离为独立模块，则通过门面接口或 REST 调用替代 |
| `MissingFieldDetector` 检测策略 | 检测报告模式 vs 自动补全模式 | 检测报告模式（列出缺失字段 + 建议补充内容，不自动填充） | 医疗病历的关键字段涉及诊断准确性，自动补全可能引入错误信息。检测报告模式将补全决策权交给医生，同时提供建议内容加速医生操作 |
| 审核结果持久化 | 仅数据库 vs 数据库 + 搜索引擎 | 数据库（`AuditRecord` JPA Entity） | Phase 2/3 范围内审核记录按挂号/处方 ID 查询即可满足需求，无需搜索引擎。Phase 5 引入分析报表时再评估 Elasticsearch 等方案 |
