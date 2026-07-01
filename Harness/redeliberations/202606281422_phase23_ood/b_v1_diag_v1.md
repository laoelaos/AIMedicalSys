# 质量审查报告：Phase 2/3 包C/D-AI1/D-AI2/E 架构级 OOD 设计

## 审查概要

- **审查对象**：`Harness/redeliberations/202606281422_phase23_ood/a_v1_design_v1.md`
- **审查轮次**：第 1 次
- **审查重点**：需求响应充分度、事实错误/逻辑矛盾、深度与完整性（侧重内部审议未覆盖维度）

整体而言，该设计文档结构完整、核心抽象覆盖了需求中的主要业务场景，四个包的模块划分和职责分配基本合理。但存在以下 7 项质量问题，其中多数属于"编码前必须澄清"级别，建议修复后再进入实现阶段。

---

## 1. DialogueSession 不可变声明与可变追加操作的逻辑矛盾

- **所在位置**：§3.1 `DialogueSession` — "为何使用不可变 class" 说明 + 协作描述
- **严重程度**：P1（高）
- **问题描述**：设计文档明确宣称 `DialogueSession` 为**不可变 class**，理由为"避免引用逃逸导致的并发问题"。但在协作描述中写明 `TriageServiceImpl` 对其进行"读取和**追加**"（追加本轮 QA 内容）。不可变对象在构造后无法被追加修改。如果设计意图是"每次追加产生新实例，由 Manager 替换旧引用"的 copy-on-write 模式，当前描述未体现这一模式，实施者会面临两难：按不可变实现则无法追加，按可变实现则与设计理由矛盾。
- **改进建议**：选择以下任一路径并更新文档：**(a)** 改为可变 class（`DialogueSession` 内部状态可变，`DialogueSessionManager` 负责并发访问控制），删除不可变理由；**(b)** 保持不可变，显式补充 copy-on-write 机制：`DialogueSession.withNewRound(...)` 返回新实例，`DialogueSessionManager` 负责原子替换。

## 2. 包E 异步 AI 建议缺少消费路径

- **所在位置**：§6.3 "包E 的异步 AI 建议" + §3.4 `PrescriptionAssistController`
- **严重程度**：P1（高）
- **问题描述**：§6.3 明确说明"AI 建议以可选字段形式在**后续查询接口中提供**"，但 §3.4 的 `PrescriptionAssistController` 仅定义了 `POST /api/prescription/assist/check-dose` 一个端点，没有定义任何后续查询接口。异步调用的 AI 建议结果生成后无处可查。前端开发者无法知晓如何获取这份异步数据——是通过轮询新端点的 GET 请求？是在 check-dose 响应中附带一个 taskId？还是通过 WebSocket 推送？当前设计中对消费路径的缺失导致该功能无法落地。
- **改进建议**：补充异步 AI 建议的消费机制，至少包括：**(a)** 定义 `GET /api/prescription/assist/suggestion/{taskId}` 查询端点及其响应结构；**(b)** 或定义事件推送机制（WebSocket/SSE）以及前端订阅方式；**(c)** 明确 check-dose 响应中是否需要返回一个 taskId 用于后续查询。

## 3. 多轮分诊中对话历史的维护责任与一致性不明确

- **所在位置**：§4.1 "多轮分诊流程" + §3.1 `TriageRequest` DTO 定义
- **严重程度**：P1（高）
- **问题描述**：`TriageRequest` 中声明包含 `history`（对话历史上下文）字段由前端发送，同时 `DialogueSession` 在服务端维护会话上下文。双方都维护对话历史，当它们不一致时（如前端发送的历史与服务端记录不同步），依赖哪一方作为真相来源？如果服务端以 `DialogueSession` 为准，则前端无需发送 `history`（仅需 sessionId 即可恢复上下文）；如果前端需发送 `history`，则服务端 `DialogueSession` 成为冗余存储。目前的设计未明确裁决规则，多个实现者对同一约束可能做出不同实现，导致数据不一致。
- **改进建议**：明确对话历史的单一真相来源：**(a)** 推荐方案：服务端 `DialogueSession` 为单一真相来源，前端只需在首轮请求时携带 `chiefComplaint`，后续请求仅携带 `sessionId`，`TriageRequest.history` 字段移至服务端内部不暴露至 API 契约；**(b)** 如确需前端维护历史（如减轻服务端内存压力），则需明确 `DialogueSession` 只维护状态元数据（轮次、超时时间）而不存储对话内容，并删除"会话上下文"的职责描述。

## 4. DosageCheckRequest 缺少给药途径参数

- **所在位置**：§3.4 `DosageCheckRequest` DTO 声明
- **严重程度**：P1（高）
- **问题描述**：`DosageCheckRequest` 包含 `drugCode`、`dosage`、`unit`、`patientAge`、`patientWeight`，但**缺少给药途径（route of administration）**。在临床实践中，同一种药品的口服与静脉注射剂量阈值相差悬殊（如氨苄西林口服成人 2-4g/日，静脉可达 8-12g/日）。缺少给药途径的剂量检查将产生临床意义上的错误告警——可能对安全的静脉剂量误报 BLOCK，或对危险的口服剂量漏报。`DosageThresholdService` 的阈值比较逻辑也需要给药途径维度才能正确查表。
- **改进建议**：在 `DosageCheckRequest` 中增加 `routeOfAdministration` 字段（枚举类型，建议值：`ORAL`、`IV`、`IM`、`TOPICAL` 等），`DosageStandard` 实体也需相应增加给药途径维度，查询时 `drugCode + routeOfAdministration` 联合定位唯一阈值记录。

## 5. prescription 模块内 DosageStandard 实体的写权限归属未定义

- **所在位置**：§2.2 "包D-AI1 与包E 的强耦合处理"
- **严重程度**：P1（高）
- **问题描述**：设计声明 `DosageStandard` 实体（药品剂量标准）由审核（D-AI1）和辅助开方（E）**两个子域共同写入/读取**。在领域设计中，同一聚合根不应有多个写入者——两方同时写入会导致数据完整性责任模糊、写入冲突和更新时序问题。如果两方持有不同的写入触发器（如辅助开方在剂量检查时触发更新、审核在规则检查时触发更新），无协调机制时一方写入可能覆盖另一方未提交的变更。此外，`DosageStandard` 作为参考数据（药品剂量上限）通常是管理员维护的静态数据，业务子域不应有写入权限。
- **改进建议**：**(a)** 明确定义 `DosageStandard` 的数据归属：推荐由管理员端（Phase 5）或独立的数据维护层作为唯一写入者，D-AI1 和 E 仅持有读取权限；**(b)** 若确有写入需求，定义明确的写入协调机制（如事件驱动的版本递增或最后写入者胜出策略），并补充写入场景的描述。

## 6. 对话会话内存存储未覆盖服务重启场景

- **所在位置**：§3.1 `DialogueSessionManager` + §7 设计决策 "多轮对话存储" 条目
- **严重程度**：P2（中）
- **问题描述**：设计决策明确选择内存 `ConcurrentHashMap` 作为多轮对话会话的存储方案，理由充分（短期交互、Phase 2/3 范围）。但设计未考虑服务重启场景：正在进行中的多轮分诊会话在应用重启后全部丢失，前端携带 sessionId 发起的后续请求将因 session 不存在而失败。`DialogueSessionManager` 的 `findOrCreate` 方法未定义 "session 不存在" 时的行为——是返回空、抛异常还是自动创建新会话？前端也未收到相应的错误码指引来处理 session 失效场景。
- **改进建议**：**(a)** 在 `DialogueSessionManager.findOrCreate` 中明确 "session 不存在" 时的处理策略（如返回 `Optional.empty()` 或抛 `BusinessException(TRIAGE_SESSION_EXPIRED)`）；**(b)** 补充统一的错误码 `TRIAGE_SESSION_EXPIRED` 及前端提示文案；**(c)** 在 API 文档中说明 session 有效期的限制。

## 7. 新模块依赖声明未包含 common-module-api

- **所在位置**：§1.2 "各模块仅依赖 common 和 ai-api" + §2.2 依赖关系图
- **严重程度**：P2（中）
- **问题描述**：Phase 0 约定所有业务模块（patient/doctor/admin）均依赖 `common`、`common-module-api` 和 `ai-api`。但本设计中的三个新模块仅声明依赖 `common` 和 `ai-api`，**未声明对 `common-module-api` 的依赖**。三个模块在实际运行时可能需要获取当前用户上下文（如 `CurrentUser` 获取登录医生/患者信息）、调用用户数据门面（如查询患者基本信息），这些能力由 `common-module-api` 提供。缺少该依赖会导致编译失败或运行时 `ClassNotFoundException`。
- **改进建议**：在 §1.2 的依赖声明和 §2.2 的依赖关系图中，补充三个新模块对 `common-module-api` 的依赖（compile scope），与现有 patient/doctor/admin 模块保持一致。

---

## 总结

共发现 7 项质量问题：**P1（高）5 项**（问题 1-5），**P2（中）2 项**（问题 6-7）。P1 问题均涉及逻辑矛盾或关键缺失，建议优先在下一轮迭代中修复。P2 问题虽不阻塞编码，但会影响生产健壮性和架构一致性，建议一并修复。
