# 质量审查报告 — v7 产出（第 7 轮）

审查日期：2026-06-29  
审查维度：需求响应充分度、逻辑正确性、深度与完整性  
审查方法：逐项比对需求文档与设计产出，验证关键行为契约的闭环性，评估落地可行性

---

## 问题清单

### 1. [严重] PrescriptionDraftContext CRITICAL 告警缺少 /assist 端点写入路径

**问题描述**：提交端点步①的 CRITICAL 阻断检查依赖 PrescriptionDraftContext 中的数据（§4.2 步①）。但 PrescriptionDraftContext 的写入触发点仅定义为"每次 check-dose 请求执行后"（§3.4 line 689）。而 /assist 主端点同样调用 DosageThresholdService 进行剂量阈值校验并产出 doseWarnings（§3.4 line 620），若其中包含 CRITICAL 级别告警，这些告警不会写入 PrescriptionDraftContext。医生通过 /assist 获取处方草案后直接提交（不走 check-dose），步①将判定为无 CRITICAL 告警而放行，导致患者安全风险。

**所在位置**：§3.4 PrescriptionAssistService 职责描述（line 619–621）、§3.4 PrescriptionDraftContext 覆盖更新行为契约（line 689）、§4.2 处方提交端点步①（line 824）

**严重程度**：严重

**改进建议**：在 /assist 主端点的本地即时校验执行后，将 alertLevel=CRITICAL 的告警同步写入 PrescriptionDraftContext（以 prescriptionId 为键全量覆盖），写入逻辑与 check-dose 路径一致。同时补充 /assist 端点未携带 prescriptionId 时的兜底处理（如由 Service 层自动生成 UUID 并回写 PrescriptionAssistResponse）。

---

### 2. [严重] DedupTaskScheduler 去重逻辑存在 TOCTOU 竞态条件

**问题描述**：check-dose 端点的 AI 异步调用去重逻辑为"查询 AiSuggestionResult → 判断状态 → 按条件创建新 task"三步操作（§3.4 line 643）。虽然 SuggestionStore 的 compute() 提供了单次写入的原子性，但**去重判定本身**由多步组成（读→比较→条件写入），不属于原子操作。两个并发 check-dose 请求可同时通过"无 PENDING task"检查，各自创建新异步调用，违反去重约束。此竞态在 Phase 2/3（ConcurrentHashMapStore）和 Phase 5（RedisStore）均存在，因为去重逻辑在 Service 层而非 Store 层。

**所在位置**：§3.4 "异步 AI 调用去重策略"（line 643）、§6.3 "包E 的异步 AI 建议与去重"（line 1305）

**严重程度**：严重

**改进建议**：在 SuggestionStore 接口新增原子性的 `putIfAbsent(taskId, result)` 或 `createIfNotExists(prescriptionId, taskFactory)` 方法，将去重判定下沉到存储层原子操作。或将去重逻辑改为"先以 PENDING 状态预创建 → 若存在则复用"的 putIfAbsent 模式。前端 300ms 防抖可降低并发概率但不能消除竞态，业务层仍需原子保证。

---

### 3. [一般] 阻断竞态防护设计中的二次 CRITICAL 验证缺少快照保存机制

**问题描述**：§4.2 阻断竞态防护段落（line 836）定义"步③执行前增加二次 CRITICAL 验证——在步②通过后、步③执行前，重新查询 PrescriptionDraftContext 中该 prescriptionId 的 CRITICAL 告警列表，与步①检查时的快照做比对"。但步①检查时未定义**快照存储位置和格式**：步①与二次验证之间存在步②执行间隙，步①检查快照保存在哪？是线程局部变量还是上下文对象？若为局部变量，步②无法访问；若为上下文对象，需明确定义 SubmitContext 闭包的数据结构。

**所在位置**：§4.2 阻断竞态防护手段（line 836）

**严重程度**：一般

**改进建议**：提交端点内部设计一个 SubmitContext 值对象（线程级闭包），在步①执行时快照当前的 CRITICAL 告警列表快照（List<DosageAlert> 或集合的时间戳哈希），步③前比对快照与实时查询结果的差异。若快照值与实时值不同（新 CRITICAL 出现），则中止提交。将快照的定义和生命周期纳入设计。

---

### 4. [一般] DeadLetterCompensationService "重新投递"的投递目标不明确

**问题描述**：DeadLetterCompensationService（§2.2 line 320）从 dead_letter_event 表读取失败记录后执行"重新投递"，但未定义投递目标——是重新发布 ApplicationEvent（即重新触发 RegistrationEventListener）还是直接调用 TriageService.selectDepartment()？若为前者，RegistrationEventListener 上的 @Retryable 注解将被再次触发，形成"@Retryable 重试→全部失败→写入死信→补偿重新投递→又触发 @Retryable"的无限循环，因为死信补偿的投递即是原监听器的入口。若为后者（直接调用 selectDepartment），则需明确 DeadLetterCompensationService 必须绕过 @Retryable 逻辑直接调用业务方法。

**所在位置**：§2.2 DeadLetterCompensationService 补偿描述（line 320）

**严重程度**：一般

**改进建议**：明确 DeadLetterCompensationService 的投递策略——建议采用"直接调用 TriageService.selectDepartment() 补偿业务逻辑"的模式，而非重新发布 RegistrationEvent。同时补充补偿执行结果的状态机(state=FAILED/COMPENSATED/EXPIRED)的迁移规则，确保补偿不会重复消费。

---

### 5. [一般] AiResult.prescriptionAssist() 超时场景下的 PartialData 类型无保证

**问题描述**：AiService.prescriptionAssist() 返回 `CompletableFuture<AiResult<PrescriptionAssistResponse>>`。超时场景下 partialData 通过 AiResult.failure(errorCode, T partialData) 传入 data 字段（§2.3 line 328），泛型 T 绑定为 PrescriptionAssistResponse。但 §3.4 AiResult→AiSuggestionResult 映射表（line 665–676）中，"超时（failure + partialData）" 行定义 partialData 为"已经生成的部分推荐结果"，"明确失败（success=false）"行定义 partialData 为 "data 携带部分结果（如已生成的处方片段）"。两行均呈现 AiResult.data 作为 partialData，但 prescriptionAssist 是长度优先的生成任务（完整处方草案含多药品），"部分结果"的语义存在歧义——是部分字段（如仅有 drugName 无 dose）还是部分药品（如 5 种药品中生成 3 种）？

**所在位置**：§2.3 AiService.prescriptionAssist() 方法定义（line 350–355）、§3.4 AiResult→AiSuggestionResult 映射表（line 665–676）

**严重程度**：一般

**改进建议**：明确 "partialData" 在辅助开方场景下的具体语义——建议统一为"已成功生成的完整药品条目列表"（即部分药品，而非部分字段），确保 AiSuggestionResult.suggestion 的 JSON 序列化一致性。若 AI 只能生成不完整的药品信息（字段缺失），视为 AI 实现缺陷而非 partialData。

---

### 6. [一般] MissingFieldDetector 的组织形式在 §1.3 与 §2.1 之间不一致

**问题描述**：§1.3 核心抽象表（line 115）将 MissingFieldDetector 定义为 `interface`（接口），其职责为"基于科室模板的必填字段列表与 AI 输出实际包含字段进行差集比对"。但 §2.1 目录结构（line 231）将其归属在 `parser/` 包下（`parser/MissingFieldDetector.java`）。接口放在 `parser/` 包中不符合常见约定——接口通常按职责分包（如 `detector/` 或 `service/`），`parser/` 暗示实现细节而非契约抽象。

**所在位置**：§1.3（line 115）vs §2.1（line 231）

**严重程度**：轻微

**改进建议**：将 MissingFieldDetector 调整至独立 `detector/` 子包，或在 `service/` 下与 MedicalRecordService 同层。interface 不应放在 `parser/` 包下。

---

### 7. [轻微] TriageRecord 未定义"重新分诊"的生命周期行为

**问题描述**：同一患者在同一 session 内重新发起分诊（如修改主诉重新咨询），TriageRecord 的写入行为未定义——是覆写已有记录（按 sessionId 或 patientId 去重）还是追加新记录？§3.1 仅描述"分诊结果返回响应前同步写入 TriageRecord"（line 400），未覆盖重新分诊场景。若追加记录，finalDepartmentId 补充写入路径如何定位"正确"的记录？

**所在位置**：§3.1 TriageRecord 写入时机（line 400）、TriageService.selectDepartment()（line 406）

**严重程度**：轻微

**改进建议**：明确 TriageRecord 的生命周期策略——建议按 sessionId 一对一映射，同 sessionId 重新分诊时覆盖已有记录（update 而非 insert），保持 TriageRecord 与 DialogueSession 的生命周期一致。selectDepartment 方法通过 sessionId 定位记录时不应有歧义。

---

## 整体评价

产出已历经多轮迭代修订，对用户需求的覆盖全面，架构决策清晰合理，核心行为契约定义较为完整。遗留问题集中在三方面：**安全闭环缺失**（问题 #1，CRITICAL 写入路径遗漏）、**并发正确性**（问题 #2，去重竞态；问题 #3，快照机制未定义）、**补偿执行可落地性**（问题 #4，补偿目标歧义）。建议优先修复 #1 和 #2，其余问题在后续迭代中补齐。
