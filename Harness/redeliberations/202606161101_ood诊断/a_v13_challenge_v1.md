# 诊断质询报告（v13）

## 质询结果

CHALLENGED

## 逐维度审查

### 1. 证据充分性

**[通过]** §1.2 JDBC scope 不一致：OOD L1407-L1411（H2 runtime）与 L1413（MySQL/PostgreSQL compile）的 scope 差异已通过逐行对照确认，诊断依据充分。

**[通过]** §2.1 错误码命名约定映射缺失：OOD §8.2 未显式列出各 AI 能力错误码清单，需求 §3.4 L814-L818 明确要求先 `_AI_` 中段，证据链完整。

**[通过]** §2.3 DTO 字段约束缺失：OOD L910（TriageRequest.chiefComplaint 无长度约束）与需求 §3.4.1 L825（5–500 字符）已对照验证；OOD L988（MedicalRecordGenRequest.dialogueText 无约束）与需求 §3.4.3 L861（50–10000 字符）已对照验证；OOD L1011（ConditionInfo.chiefComplaint 无约束）与需求 §3.4.4 L890（≥5 字符）已对照验证。证据充足。

**[通过]** §3.2 PasswordEncoder 共享 Bean 归属：OOD §4.5 L780 明确"共享给两个 profile 的 SecurityConfig 使用"，报告描述与 OOD 原文一致。

**[问题-一般]** §1.1 中报告将 `common` 模块需要 `spring-boot-starter-security` 的"实际原因"归因于 `GlobalExceptionHandler` 需要捕获 `AuthenticationException` 和 `AccessDeniedException`。但 OOD 中：(a) §3.1 (L492-L501) GlobalExceptionHandler 的协作对象列中未列出这两种异常类型；(b) §4.5 L784 明确认证/授权错误由 SecurityConfig 的 AuthenticationEntryPoint/AccessDeniedHandler 处理（在 application 模块），而非由 @ControllerAdvice 的 @ExceptionHandler 捕获；(c) OOD §2.2 L307 自身的解释是"部分通用安全类型仍可能被复用"。报告将未经 OOD 文档验证的推测表述为"实际原因"，证据链条在此处存在断层——该断言若属实需额外证据支撑，否则仅为推测。

### 2. 逻辑完整性

**[通过]** §2 内部优先级判定逻辑自洽：P0=阻塞验收标准 / P1=影响衔接质量 / P2=建议性改善的三级标准统一执行，§2.1、§2.2、§2.3 均按此标准判定。§2.1 从 P0 降为 P1、§2.2 维持 P1、§2.3 维持 P1，与 §2 开头统一标准一致。

**[通过]** §3.2 PasswordEncoder 风险提示逻辑一致：共享 Bean 无需激活特定 Profile，测试建议取消 `!phase0` 前提后与原设计一致。

**[通过]** §8.3 优先级调整逻辑自洽：从 P1 降为 P2，新增"优先级判定自检"段落解释降级依据，与 §8.2（P2）判定逻辑一致。

**[问题-轻微]** §8.3 的"优先级判定自检"段落作为自检机制试点仅出现在该节一处，§2.1、§2.2、§2.3 等含 P1/P2 标记的章节未附带同级别自检说明。报告自述"后续每含优先级标记的章节在修订时均需在末尾附带自检说明"，但当前版本未在一处试行之外的其他位置同步该格式。该不一致属于报告格式层面的自恰性缺口，不影响根因定位正确性。

### 3. 覆盖完备性

**[通过]** 任务描述要求的 5 类诊断维度（定义矛盾 §1、偏离需求文档 §2、反向边界检查 §3/CD、技术风险提示 §4、偏离路线图 §7）均显式覆盖，无遗漏。

**[通过]** 历史迭代反馈中的 5 项问题（§3.2 Profile 假设、§2.3 断言精度、§2.2 边界指引、§8.1 外部依赖、§8.3 优先级）在本版中均有响应修正记录（v13 修订说明），覆盖充分。

**[通过]** 用户原始需求（requirement.md）中所有问题类型的审查结论均有显式声明，包括 §6 事实错误/逻辑错误维度声明和 §7 偏离路线图审查声明。

## 质询要点

### 问题：§1.1 "实际原因"断言证据不足

- **问题**：报告将 §1.1 中 security 依赖矛盾的"实际原因"归因为 GlobalExceptionHandler 的 @ExceptionHandler 需要捕获 AuthenticationException/AccessDeniedException（引用 §5.1 L806-L807 为据），但 OOD 中 GlobalExceptionHandler（§3.1 L492-L501）未声明对这两种类型的 @ExceptionHandler 方法，OOD §4.5 L784 明确认证/授权错误由 SecurityConfig 的 AuthenticationEntryPoint/AccessDeniedHandler 处理（在 application 模块而非 common 模块），且 OOD §2.2 L307 为 security 依赖提供了自己的解释（"部分通用安全类型仍可能被复用"）。报告将该推断性断言表述为"实际原因"，缺乏 OOD 文档或代码层面的直接证据支撑。

- **原因**：该断言若不准确，将使修复者须知中的修正措辞（"用于 GlobalExceptionHandler 捕获安全异常类型"）建立在不完全准确的前提上。虽然核心诊断结论（L293 与 L306-L307/L785 的矛盾陈述）仍然成立，但"为何矛盾"的解释缺乏可靠证据，降低报告该节的可信度。

- **建议方向**：删除或弱化"实际原因是"的断言表述，改为更审慎的推测或直接引用 OOD L307 的原文解释（"部分通用安全类型仍可能被复用"）作为 `common` 依赖 security 的文档化理由。若认为 GlobalExceptionHandler 是备选解释，应在声称前验证 OOD 中对 GlobalExceptionHandler 方法的描述是否确实涉及这些异常类型，或在诊断中标注为推测而非事实。
