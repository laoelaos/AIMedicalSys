# 技术方案审查报告（v7）

## 审查结果

APPROVED

## 逐维度审查

### 1. 技术准确性

**[通过]** 技术选型与库能力描述准确：Spring Boot 3 多模块工程、Vue 3 + TS + Vite + Pinia、Spring Security + JWT（BCrypt 强哈希）、Spring Data JPA + SQL-Toy 双栈、Spring AI ChatModel、Spring AI Structured Output、OpenFeign / Sleuth / Seata（Nacos/Eureka + Spring Cloud Gateway + Nacos Config 微服务兼容）、Conventional Commits、HL7/DICOM/串口/网关四类设备协议、RAG 形态医学知识库、向量库管理、WebSocket 高风险告警 + SSE 病历流式输出等选型均与场景契合，无虚构或误用。

**[通过]** 类型系统与机制描述在语言能力范围内：`PENDING_PHASE4_DISPATCH` 状态枚举值、`registration_type=OUTPATIENT/EXAMINATION` 双分支、状态机五态/六态/四态、鉴权拦截器、事务回滚等描述符合 Spring Boot / JPA / Java 类型系统的实际能力边界。

**[通过]** AI 能力与降级路径描述与 Spring AI 实际能力一致：3.4.1 智能分诊（Spring AI ChatModel + Prompt 模板 + 超时降级）、3.4.3 AI 病历生成（Structured Output + 科室 Prompt 模板）、3.4.2 AI 处方审核（三档风险 + 6 秒硬超时）等均未超出 Spring AI 现有能力边界。

### 2. 完备性

**[通过]** 用户任务 §2 硬约束全部承接：
- 「骨架最小化 + 多人并行」— Phase 0.2 协作规范行从"文件名占位"升级为"可执行流程级规范"（分支命名 / Commit 格式 / PR 模板 / CR 必查项 / 配套文件清单），多人并行贡献的流程侧实质化（S-2 必修）已完成。
- 「接口契约冻结粒度」— Phase 0.2 增列"接口契约冻结时机表"（框架级 / 模块级端点 / 模块级端点字段细节 / 状态机契约四档），S-1 必修已完成。
- 「骨架阶段最小化」— Phase 0.2 硬必需条目数从 v6 的 7 项降到 6 项（持续集成由硬必需下沉到软预备），S-3-a 应修已完成。
- 「§4.6 任务分层覆盖」— 阶段间总览对照表后新增独立"任务分层 ↔ Phase 映射表"小节（核心任务一～六 + 6 项拓展任务），P2-3 必修已完成。

**[通过]** 需求 §1.4 六大业务域（患者服务 / 门诊诊疗 / 检查检验 / 药房药库 / 医院管理 / AI 能力）全覆盖；§2 三大终端角色 + 医生 5 子岗位（门诊 / 检查 / 检验 / 药房 / 线下接诊）全覆盖；§3 各功能模块（3.1.x / 3.2.x / 3.3.x）通过 Phase 0~6 全部承接；13 项 AI 能力（3.4.1~3.4.13）按"核心→拓展"分层在 Phase 2~5 完成"首次落地"，每项均有显式降级路径声明（协作硬约束表"13 项 AI 能力均提供降级路径"行已落地）。

**[通过]** 数据流闭环完整：挂号→分诊→接诊→AI 审核/病历→缴费→发药→退药→退费全链路（Phase 6 收口）；EXAMINATION 占位申请 Phase 2 → Phase 4 派单衔接规则（`PENDING_PHASE4_DISPATCH` + 首次回填）明确；3.1.5 / 3.1.7 / 3.1.8 占位 → 真实数据接入分阶段说明（Phase 2 占位 / Phase 4 真实接入）已显式承接；3.1.3.2 病情咨询 Phase 2 Mock 兜底 → Phase 5 真实 3.4.8 调用两增量明确；调拨 / 盘库 / 退药状态机 + 角色矩阵 + 事务边界下沉 OOD 边界清晰。

**[通过]** 风险与依赖（路线图层面战略风险）— 5 行战略风险表覆盖 Phase 4 工作量集中、骨架阶段误读、13 项 AI 降级路径完整性、微服务化拆分、底座演进兼容性；附录 D 沉淀 14 行实施战术级风险作为索引。

### 3. 可操作性

**[通过]** Phase 0.2 协作规范行可执行性高：分支命名（`feature/<TICKET-ID>-<short-desc>` / `fix/...` / `hotfix/...`）、Commit 格式（Conventional Commits 6 种 type）、PR 模板（变更范围 / 影响模块 / 接口契约变更 / 数据库迁移 SQL / 测试覆盖 / 截图或录屏）、CR 必查项（接口契约 / 迁移 SQL / 文档同步 / 单测 / 跨模块 owner 评审）、配套文件（README / CONTRIBUTING / AGENTS / PR 模板 / CODEOWNERS）均具体到文件名级，开发者可直接落地。

**[通过]** 接口契约冻结时机明确：框架级 Phase 0 冻结、模块级端点 Phase 1 启动前冻结、模块级端点字段细节随各业务模块首次提交时冻结、状态机契约路线图层面已冻结（字段下沉 OOD）——下游可明确"何时冻结什么"。

**[通过]** 每个阶段"阶段目标 / 阶段交付物 / 关键技术决策 / 可演示能力 / 可分配子任务包 / 阶段准入准出"六要素结构清晰，子任务包可分配给不同开发者（Phase 3 拆 D-AI1 / D-AI2；Phase 4 七条关键路径 + 5 位开发者建议；Phase 5 七包；Phase 6 六包）。

**[通过]** AI 进阶底座 Phase 5 / Phase 6 职责分工（v5 收口，v6 压缩为 3 行对照表）措辞一致；3.4.7 影像分析 Phase 4 分析推理 / Phase 5 模型管理面两阶段边界清晰；落地阶段定义（v5 收口）作为统一语义口径，准出口径"在本阶段落地 N 项"严格按此核计。

**[通过]** 持久化双栈（JPA / SQL-Toy）使用场景明确：Phase 4 检验趋势图、Phase 5 排班统计、Phase 6 性能指标均标注 SQL-Toy；CRUD 主路径 JPA；与 Phase 0.3 持久化方案决策行闭环。

**[通过]** 关键路径与里程碑（Phase 4.1 增列，v5 收口）将 9 个任务包分为 7 条关键路径，路径 1/2/5/6 可并发，路径 3→4→7 串行，5 位开发者建议具体可执行——回应 P2-1 工作量集中风险。

**[轻微]** P2-4 修复采用"约定段落代替逐一下沉"策略：Phase 0.3 关键技术决策前新增"路线图层面 OOD 级别命名约定"段落（v7 收口），明确"这些命名在路线图层面仅作'协作语义锚点'使用，行为契约的语义优先于具体命名；OOD 阶段可对类名 / 字段名 / 状态枚举值 / API 路径作最终调整"——从实质上回应了"不要让下游误以为这些命名已冻结"的问题（OOD 设计者明确知道可自由调整命名），但路线图层面仍存在 30+ 处 OOD 级别命名（如 `TriageRuleProvider` / `ExamRecommendRuleProvider` / `PENDING_PHASE4_DISPATCH` / `allergy_history` / `allergy_details` / `reaction_type` / `severity` / `api/triage/consult` / `knowledge/qa` / `Result<T>` / `BaseEntity` / `GlobalExceptionHandler` / `comorbidities` / `item_id` / `exam_application` 等）。**不构成 REJECTED 理由**——技术决策明确，行为契约描述完整；OOD 阶段可按"约定段落"授权自由调整命名，不阻塞实现启动。

**[轻微]** S-3-b 修复采用"保持 0.7 但未做大改"策略：Phase 0.7 节仍存在（子节数 7 未降到 6），与 Phase 0.6 准出条款的"软预备条目说明"以及 Phase 0.2 / 0.3 / 0.5 表格的"层级"列存在部分字段重复。**不构成 REJECTED 理由**——硬必需条目数 6 已达成（达到 S-3 修复主目标），冗余部分不影响实现者对 Phase 0 软预备条目"已固化位置 / 已引入依赖"为达成标志的判读。

**[轻微]** P2-1 修复未做：Phase 4.5 "包 D（3.4.4 / 3.4.9 真实 AI 推理承接）"未重命名为 D-AI（与 Phase 3.5 D-AI1 / D-AI2 命名风格对齐）。**不构成 REJECTED 理由**——P2-1 为最低优先级的"可修"类问题；当前 Phase 4.5 包 D 与 D1（药库） / D2（调拨）已通过括号内的内容描述"3.4.4 / 3.4.9 真实 AI 推理承接"明确解耦，同阶段命名混淆风险低。

## 修复要求

无（APPROVED 通过）。
