# 质量审查报告 — a_v3_diag_v1.md

## 审查范围 & 视角

- **待审查产出**：Phase 23 C/3/DE 验收问题定位诊断报告（v4）
- **审查维度**：需求响应充分度、事实错误/逻辑矛盾、深度与完整性、可操作性（修复建议是否可执行、副作用分析、优先级合理性）
- **不重复验证**：内部审议已覆盖的技术可行性等维度

---

## 发现的问题

### 问题 1 — 事实错误：OOD 节号引用错误

- **问题描述**：报告中 2 处引用 "OOD §3.3 错误码表"（P05 条目第 1 段第 3 行、修订说明 Q3 回应），但 OOD 文档 (`Docs/07_ood_phase2_C_3_DE.md`) 的 §3.3 是「包D-AI2：病历生成」，并非错误码表。实际的错误码表位于 OOD **§5.1 模块级错误码**（line 1432-1460）。
- **所在位置**：`a_v3_diag_v1.md:188`（P05 `[代码修改]` 段）、`:416`（修订说明 Q3）
- **严重程度**：轻微
- **改进建议**：将 2 处引用统一修正为 "OOD §5.1 错误码表"，确保执行者按图索骥时不会查错节号。

### 问题 2 — 逻辑矛盾 & 修复建议不可执行：P05 错误码处理方向未给出确定建议

- **问题描述**：P05 中针对 `RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT` 给出建议 "需要新增该错误码或改用现有已定义错误码"，这是一个二选一的模糊建议，执行者无法直接行动。更关键的是：该段 [代码修改] 与同条目 [OOD 文档修改] 存在自洽矛盾——[OOD 文档修改] 已在 `SubmitResponse` 中新增 `warnResult` 字段，[代码修改] 相应应当是将 WARN 路径改造为通过 `warnResult` 承载风险信息并移除该错误码（或将其降级为纯内部标记），而非在错误码表中新增一个本应被 `warnResult` 替代的错误码。
  - OOD §5.1 的错误码表中 `RX_AUDIT_` 前缀对应 HTTP 409/422，而 WARN 路径的"提示医生选择"语义本质是 200 + warnResult 业务负载，不是错误场景，不应走错误码路径。
  - 原有审查记录 (review_v1_2.md:101, todo.md:49) 已明确建议 "填充 warnResult 并设 submitted=false、errorCode=null" 和 "移除或重新命名该错误码"，本报告采纳了 warnResult 方案但未相应清理错误码路径的模糊表述。
- **所在位置**：`a_v3_diag_v1.md:188`（P05 `[代码修改]` 段末句）
- **严重程度**：一般
- **改进建议**：将 "需要新增该错误码或改用现有已定义错误码" 改为明确的单向建议。推荐方案：WARN 路径不再使用 errorCode，改为通过 `SubmitResponse.warnResult` 承载风险信息（riskLevel/alerts/auditRecordId/prescriptionHash），`errorCode=null`；`RX_AUDIT_WARN_REQUIRES_FORCE_SUBMIT` 错误码从代码侧移除。若保留该错误码用于其他内部追踪用途，应在 OOD §5.1 错误码表中补登并说明其使用场景仅限于 `forceSubmit=true` 校验失败且 `warnResult` 不可用时的兜底降级路径。

### 问题 3 — 关键遗漏：A07 与 A09 的跨问题耦合未分析

- **问题描述**：跨问题耦合副作用分析章节仅覆盖了 C04×E02×S04 一组耦合，但未分析 A07 与 A09 之间的相互约束：
  - A07 指出 `AiResult.success(data)` 不校验 `data=null`，违反契约。
  - A09 指出 `AuditConverter.toAuditResponse` 在 `aiData==null` 时退化为 PASS+空alerts，建议修改为调用方先检查 aiResult 有效性再调 Converter。
  - **耦合关系**：若 A07 先被修复（在 `success()` 中添加 `Objects.requireNonNull(data)` 断言），则 `aiResult.isSuccess() && aiResult.getData()==null` 的场景将不再出现，A09 当前的 null-handling 路径被消除（变为不可达）。反之若 A09 先被修复（调用方前置检查 + 走降级路径），则 A07 的修复变为低优先级。
  - **副作用**：若两者独立并行修复，A09 的调用方前置检查与 A07 的非空断言在修复后可能产生冲突（降级路径中 `AiResult.failure(..., partialData)` 的 partialData 为 null 时，Assert 可能误拦截）。
- **所在位置**：`a_v3_diag_v1.md:392-408`（跨问题耦合副作用分析章节），涉及条目 `:366-383`（A07）和 `:305-308`（A09）
- **严重程度**：一般
- **改进建议**：在跨问题耦合副作用分析中增加 A07×A09 耦合条目，约定修复顺序为：(1) 先修复 A09 的调用方前置检查 + 降级路径（使 null-data 不再流入 Converter）；(2) 再修复 A07 的 `Objects.requireNonNull(data)` 断言（此时 null-data 路径已消除，断言不会误拦截降级场景）；(3) 注意 `AiResult.failure()` 返回的 `partialData` 可能为 null，不应受此断言约束。

### 问题 4 — 修复建议不够可操作：A10 缺少具体修复指引

- **问题描述**：A10（application.yml 完全缺失 ai.timeout.* / facade.*.timeout / ai.mock.* 配置项）仅有证据陈述 "application.yml:1-14 仅含 JWT 配置"，未给出需要添加的具体配置键列表和默认值。与其他 P0/P1 条目（如 C04、C07、C08 均有详细修复方案和行号范围）相比，A10 的修复指引过于抽象。执行者拿到 A10 条目后仍需要去 OOD §5.5 和 §2.3 逐行查找所有需外化的超时配置。
- **所在位置**：`a_v3_diag_v1.md:375-378`（A10）
- **严重程度**：轻微
- **改进建议**：A10 补充需要新增的完整配置键列表，参考 OOD §5.5（line 1476-1479）和 §2.3 各 AI 能力超时段落：
  - `ai.timeout.triage=8s`
  - `ai.timeout.prescription-audit=6s`
  - `ai.timeout.medical-record-generate=12s`
  - `ai.timeout.prescription-assist=8s`
  - `consultation.doctor-facade.timeout=2s`
  - `medical-record.visit-facade.timeout=2s`
  - `ai.mock.response-strategy=STATIC`（mock 模式下）

### 问题 5 — 可操作性局限：C08 修复方向未说明时序依赖

- **问题描述**：C08 修复建议要求 "RegistrationEventListener 调用前自行检查 TriageRecord.finalDepartmentId 是否为空，仅当为空时调用 selectDepartment()"。但 EventListener 在执行此检查时，需要先确保 `TriageRecord` 已创建（即在 triage 流程中完成持久化）。若 RegistrationEvent 在 triage 完成前触发（罕见时序竞争），TriageRecord 尚不存在，`findBySessionId` 返回 null，EventListener 无法判断是该跳过还是等待。报告未分析此时序窗口的风险。
- **所在位置**：`a_v3_diag_v1.md:118`（C08 修复方向第 4 段）
- **严重程度**：轻微
- **改进建议**：在 C08 修复方案中补充时序假设前提：`RegistrationEventListener` 依赖三阶段完成顺序 (a) triage 流程完成并持久化 TriageRecord → (b) 前端进入挂号界面 → (c) registration 模块发布 RegistrationEvent。指明在前端保留 sessionId 并在挂号时回传的交互设计中，此顺序由前端流程保证。若需防御性处理，可在 EventListener 中增加 `TriageRecord` 不存在时静默跳过（不做 selectDepartment）、记录 WARN 日志。

### 问题 6 — 深度不足：P09 OOD 文档修改未枚举全部受影响位置

- **问题描述**：P09 建议在 OOD 中为 PrescriptionItem 补充 unit 字段，但仅笼统说 "OOD 应在 PrescriptionItem 定义中补充 unit"。PrescriptionItem 在 OOD 中被多个位置引用，每个位置的字段列表不同，需要逐个更新：§1.3 SubmitRequest（line 110）、§3.2 PrescriptionItem 正式定义（实际为 PrescriptionAssistResponse 的 drugs 数组定义 line 811）、§4.6 提交流程中的 PrescriptionItem 引用等。报告未给出完整清单。
- **所在位置**：`a_v3_diag_v1.md:342-343`（P09 `[OOD 文档修改]`）
- **严重程度**：轻微
- **改进建议**：在 P09 [OOD 文档修改] 中枚举所有需要添加 unit 的 OOD 位置，至少包括：§1.3 SubmitRequest/SubmitResponse 中的 PrescriptionItem 引用、§3.2 PrescriptionItem 正式定义（含 AuditRequest 和 PrescriptionAssistResponse 中的 drugs 数组）、§4.6 提交流程中的 PrescriptionItem 引用、§8.3 单位一致性校验中的字段说明。

---

## 整体质量评价

报告总体质量较高：61 个问题条目均标注了真伪性、根因分类、代码证据，优先级分组合理（P0 确实包含阻塞性缺陷），跨问题合并修复策略清晰（C06/E03、C15/E01 等合并恰当）。修订说明对 8 项质询逐一回应，v4 相对前序版本有实质性改善。

主要短板集中在三方面：
1. **事实准确性**：有一处 OOD 节号引用错误（§3.3 → §5.1），虽然不影响诊断结论，但可能误导执行者查档路径。
2. **修复建议的可操作性**：P05 的二选一表述是唯一一处 "执行者拿到后仍需要自己做设计决策" 的模糊指引，与整体 60+ 条目的清晰度形成反差。
3. **耦合分析覆盖面不足**：仅分析了 C04×E02×S04 一组耦合，遗漏了 A07×A09 这组典型的前后序依赖。

以上 6 个问题均在修复者可理解的范围内，不影响报告的总体诊断价值，建议在下一轮迭代中修正。
