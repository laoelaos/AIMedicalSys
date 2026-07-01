# R1: consultation 模块（包C 智能分诊）代码审查

审查时间：2026-07-01

### 审查范围

- `api/TriageController.java`
- `service/TriageService.java` + `impl/TriageServiceImpl.java`
- `dialogue/DialogueSession.java` + `DialogueSessionManager.java`
- `rule/TriageRuleEngine.java` + `DefaultTriageRuleEngine.java`
- `fallback/DepartmentFallbackProvider.java` + `StaticDepartmentFallbackProvider.java`
- `entity/TriageRecord.java` + `DeadLetterEvent.java`
- `event/RegistrationEventListener.java`
- `service/DeadLetterCompensationService.java`
- `converter/TriageConverter.java`
- 全部 DTO (`TriageResponse`, `DialogueCreateRequest`, `RecommendedDepartment`, `RecommendedDoctor`, `MatchedRule`, `AdditionalResponse`)
- `config/SchedulingRetryConfig.java`, `exception/TriageErrorCode.java`, `rule/MatchResult.java`
- `rule/entity/TriageRule.java`, `repository/TriageRecordRepository.java`, `repository/DeadLetterEventRepository.java`, `repository/TriageRuleRepository.java`
- 全部测试文件（15 个测试类）
- 跨模块引用：`SessionStore.java`, `DoctorFacade.java`, `AvailableDoctor.java`, `RegistrationEvent.java`

### 发现

#### [严重] correctedChiefComplaint 显式/隐式路径优先级反转

- **位置**：`service/impl/TriageServiceImpl.java:148-150`
- **描述**：设计文档 §1.3 明确规定两条 correctedChiefComplaint 触发路径的优先级："显式路径优先于隐式路径——若前端传入 correctedChiefComplaint，忽略 ai-api 返回的隐式识别结果"。但实现中 line 110 先写入 request 值，line 148-150 无条件用 ai-api 返回值覆盖，导致显式路径值被隐式路径覆盖。同时 `TriageConverter.java:107-109` 也存在相同问题。测试 `TriageServiceImplTest.java:308-321` 的断言同样编码了错误的优先级期望。
- **建议**：在 line 148 处检查 `request.getCorrectedChiefComplaint()` 是否已存在，仅当 request 未携带显式值时才接受 AI 返回的隐式值。修正 Converter 中的侧写逻辑。同步修正测试断言。

#### [一般] TriageController.selectDepartment 应当使用 @RequestBody 而非 @RequestParam

- **位置**：`api/TriageController.java:30-36`
- **描述**：设计文档明确要求 selectDepartment 端点请求体包含 `{ sessionId, departmentId, departmentName }`（对应 POST JSON body），但实现使用了三个独立的 `@RequestParam`，将参数放在查询字符串而非请求体中。与设计契约不一致。
- **建议**：创建一个 `SelectDepartmentRequest` DTO 封装三个字段，改用 `@Valid @RequestBody` 接收。

#### [一般] DialogueSessionManager.restoreSession 缺少并发保护

- **位置**：`dialogue/DialogueSessionManager.java:50-71`
- **描述**：`createSession` 方法已使用 `synchronized` 保证并发安全，但 `restoreSession` 方法未加锁。当多个线程同时调用 `restoreSession` 访问同一不存在的 session 且存在对应 TriageRecord 时，可能重复创建并覆盖存储。同时缺少 TTL 过期后应返回 `TRIAGE_SESSION_EXPIRED` 的语义——当前返回 `null` 后由调用方视为 `TRIAGE_SESSION_NOT_FOUND`，与设计文档的 TTL 竞态处理描述不符。
- **建议**：对 `restoreSession` 加 `synchronized` 保持与 `createSession` 一致；增加过期检测并返回 `TRIAGE_SESSION_EXPIRED` 错误。

#### [一般] TriageServiceImpl 降级路径手工构造 Response 而非使用 Converter

- **位置**：`service/impl/TriageServiceImpl.java:167-181`
- **描述**：AI 失败后的降级路径手工构造 `TriageResponse` 对象并填充各字段，未复用 `TriageConverter.toTriageResponse()`。导致降级路径与正常路径的 Response 构建逻辑分散，维护成本增加。且降级路径遗漏了 `matchedRules` 字段的填充。
- **建议**：降级路径应复用 Converter 或提取公共构建方法，并在降级响应中携带规则匹配结果（matchedRules）。

#### [一般] TriageConverter 存在修改 Session 对象的副作用

- **位置**：`converter/TriageConverter.java:107-109`
- **描述**：`toTriageResponse` 方法在转换数据时修改了传入的 `DialogueSession` 参数（`session.setCorrectedChiefComplaint(...)`），违反了 Converter 层仅做数据映射、不应产生业务副作用的职责划分原则。且该侧写在 `TriageServiceImpl` 中已有重复逻辑（line 148-150）。
- **建议**：移除 Converter 中的 `session.setCorrectedChiefComplaint` 调用，由 Service 层统一管理 Session 状态变更。

#### [一般] DialogueSession 的并发控制模式不一致且冗余

- **位置**：`dialogue/DialogueSession.java`
- **描述**：`DialogueSession` 混合使用了三种并发机制——`synchronized` 方法（大多数字段 getter/setter）、`AtomicInteger`（`aiFailCount`、`roundCount`）、`CopyOnWriteArrayList`（`additionalResponses`）。设计文档明确将并发控制职责委托给 `DialogueSessionManager` 而不是 Session 自身。当前冗余的同步方式增加维护负担，且 `synchronized` 方法与 `AtomicInteger` 的混合使用未形成统一的内存可见性保证。
- **建议**：统一移除 `synchronized` 关键字（保留 `AtomicInteger` + `CopyOnWriteArrayList`），或将所有状态变更收拢到 `DialogueSessionManager` 的同步块中。

#### [轻微] 降级兜底文案与设计文档不一致

- **位置**：`service/impl/TriageServiceImpl.java:177`
- **描述**：设计文档规定 `aiFailCount >= 3` 时的 fallbackHint 文案为 `"建议直接联系线下接诊窗口"`，实际实现为 `"AI 服务持续不可用，建议稍后重试"`。后者语义偏技术化，前者更符合患者端使用场景。
- **建议**：按设计文档统一为 `"建议直接联系线下接诊窗口"`。

#### [轻微] selectDepartment 返回的 TriageResponse 缺少科室和医生信息

- **位置**：`service/impl/TriageServiceImpl.java:294-301`
- **描述**：`toTriageResponse(TriageRecord)` 仅设置了 `sessionId`、`reason`、`confidence`、`degraded` 四个字段，未包含 `departments`、`doctors` 等分诊结果的核心字段。即使 selectDepartment 的核心目的是写入最终选科，返回历史分诊结果信息对前端展示仍有价值。
- **建议**：视前端需求补充反序列化 JSON 快照字段（`aiRecommendedDepartments`/`ruleMatchedDepartments`、`recommendedDoctors`）到响应的可选方案。

### 本轮统计

| 严重程度 | 数量 |
|---------|------|
| 严重 | 1 |
| 一般 | 5 |
| 轻微 | 2 |

### 总评

consultation 模块整体实现与 OOD 设计文档对齐度良好，核心接口、实体、DTO 均正确映射。`TriageRuleEngine` → `DepartmentFallbackProvider` 降级链路完整实现，会话管理（`DialogueSessionManager` + `SessionStore` 接口隔离）、事件补偿（`@Retryable` + 死信队列）等关键机制均已到位。测试覆盖全面（15 个测试类），核心业务路径、降级路径、边界场景均有验证。

存在一个严重问题：`correctedChiefComplaint` 显式/隐式路径优先级反转（已编码到测试断言），需优先修复。架构层面（Store 接口隔离、DoctorFacade 跨模块门面、依赖方向）合规，模块仅依赖 `common`、`common-module-api`、`ai-api`，无跨模块 impl 层依赖。
