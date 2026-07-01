# 质量审查报告：Phase 2/3 OOD 设计方案 v2

## 审查范围

- **任务**：审查 Phase 2/3 包C/D-AI1/D-AI2/E 的架构级 OOD 设计方案 v2
- **审查视角**：需求响应充分度、事实/逻辑正确性、实现就绪度
- **已知约束**：该产出已通过组件A的内部"设计-验证"审议（覆盖技术可行性等维度），本审查侧重内部审议未覆盖的维度

---

## 审查发现

### 问题1：BLOCK 风险等级的强制阻断执行机制未定义

- **问题描述**：§3.2 声明 AuditRiskLevel.BLOCK "决定前端行为"，将阻断执行完全推给前端。当前架构缺少后端的强制阻断层——一个被篡改或绕过验证的前端可以直接提交被 BLOCK 的处方，BLOCK 失去实际约束力。处方审核的安全性不应依赖前端履约。
- **所在位置**：§3.2 AuditRiskLevel 职责定位；§4.2 审核场景流程
- **严重程度**：严重
- **改进建议**：在业务层（PrescriptionAuditService 或 PrescriptionAuditController）定义独立的强制阻断切入机制。至少包括两种路径之一：
  - **路径A（推荐）**：将处方提交拆分为"预检"和"提交"两个端点。"预检"返回 BLOCK 时禁止调用"提交"端点，由后端统一拦截
  - **路径B**：在 Controller 层增加 BLOCK 结果的后端处理：当 AOP 切面或拦截器检测到审核结果为 BLOCK 时，直接返回 403/422 拒绝提交。前端仅消费错误响应，不参与阻断决策

### 问题2：AuditRecord 缺少业务关联标识

- **问题描述**：§3.2 AuditRecord 定义为"保存每次审核的原始处方、AI 结果、风险等级和时间戳"，但缺少处方级关联字段（prescriptionId / orderId）、处方医生标识（doctorId）和患者标识（patientId）。这使得审核记录无法与具体处方、医生、患者关联，审计追踪能力严重受限——无法回答"哪个处方被阻断"、"哪个医生的处方频繁触发 WARN/BLOCK"、"哪个患者涉及高风险用药"等关键问题。
- **所在位置**：§3.2 AuditRecord 协作描述
- **严重程度**：严重
- **改进建议**：在 AuditRecord entity 中增加以下必填字段：
  - `prescriptionOrderId`（处方单号）
  - `doctorId`（开方医生）
  - `patientId`（目标患者）
  - 以上字段在审核时由 PrescriptionAuditService 从 PrescriptionAuditController 传入的请求中提取

### 问题3：AiSuggestionResult 内存存储未覆盖服务重启场景

- **问题描述**：§6.3 将异步 AI 建议结果暂存在 ConcurrentHashMap 中，服务重启后所有 taskId 对应的 AiSuggestionResult 全部丢失。前端通过 GET 端点查询时返回 404 或空结果，没有任何错误提示。这与 §3.1 DialogueSessionManager 的内存存储问题（上一轮迭代反馈第8条，已修复）属于同一模式，但 AiSuggestionResult 未被修复。
- **所在位置**：§6.3 "包E 的异步 AI 建议"
- **严重程度**：一般
- **改进建议**：参照 §3.1 findOrCreate 的三分支处理模式，为异步 AI 建议补充：
  - 建议结果查询端点返回建议不存在（服务重启/TTL 过期）时的明确错误码 `RX_ASSIST_SUGGESTION_EXPIRED` 或 `RX_ASSIST_SUGGESTION_NOT_FOUND`
  - 说明建议结果的存续期（TTL），建议与 DialogueSession 保持一致（30 分钟）
  - 前端在收到建议过期/不存在时应有降级展示文案

### 问题4：DosageStandard 实体结构未定义年龄/体重分级剂量支持

- **问题描述**：§3.4 DosageThresholdService 声称"按药品编码、给药途径、年龄、体重维度检查剂量是否超限"，但 §2.1 目录结构中 DosageStandard entity 仅标注为跨模块共享实体，未定义任何字段，尤其缺少支持年龄/体重分级的剂量存储结构。若 DosageStandard 仅存储单条 maxDosage 记录（如"成人一次0.5g"），则无法支持儿童/老年人/低体重患者的差异化剂量验证。剂量标准的数据模型结构直接影响 DosageThresholdService 的校验实现逻辑，这是实现就绪度的关键缺口。
- **所在位置**：§2.1 common/entity/DosageStandard.java；§3.4 DosageThresholdService
- **严重程度**：一般
- **改进建议**：明确定义 DosageStandard 对年龄/体重分级的支持方式，建议以下之一：
  - **方式A**：DosageStandard 添加年龄范围字段（minAge, maxAge, ageUnit）和体重范围字段（minWeight, maxWeight, weightUnit），单药多条记录覆盖不同人群
  - **方式B**：拆分出 AgeBandedDosage 子实体，支持多年龄段剂量配置
  - 无论何种方式，需在 §3.4 DosageThresholdService 的 check 方法契约中明确剂量查找的优先级策略（精确匹配→年龄范围覆盖→默认成人剂量→无标准返回）

### 问题5：病历生成降级策略不合理

- **问题描述**：§4.3 定义病历生成的降级路径为"返回空病历框架 + 全字段缺失"。这意味着即使 AI 成功提取了部分结构化字段，降级时也全部丢弃并标注为缺失。从实际临床场景看，医生已输入大量对话内容，仅因 AI 超时就完全丢失结构化输出，体验极差且临床不可接受。更合理的做法是保留已提取的结构化字段，仅标注未提取的字段。
- **所在位置**：§4.3 病历生成场景——降级流程
- **严重程度**：一般
- **改进建议**：将降级策略改为"分层保护"：先尝试从 AI 的 partial/incomplete 输出中提取可用字段，仅对确实缺失的字段标记为缺失。若 AI 完全无响应或返回错误，再回退到科室模板的空框架 + 全部标记待填，而非"全字段缺失"的措辞。

---

## 整体评价

设计方案对用户需求的覆盖充分（四个包的映射清晰），上一轮迭代的 10 条反馈也已全部正确修复。当前遗留问题主要集中在：安全执行机制（问题1）、数据追溯能力（问题2）、边缘场景完整性（问题3、4、5）三个维度。建议优先处理问题1和问题2，它们在实现前必须澄清，否则将构成安全隐患和审计合规风险。
